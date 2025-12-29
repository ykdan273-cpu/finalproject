package com.example.vacancyscraper.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Сравнение способов записи/чтения бинарных записей фиксированного размера:
 * RandomAccessFile, FileChannel + ByteBuffer и memory-mapped файлы.
 *
 * Формат хранения: бинарный, 64 байта на запись (long id, long timestamp, int salary,
 * long companyHash, long cityHash, long score, long offset, padding 3 x int).
 */
public class FileIOBenchmarkTest {

    private static final int RECORD_SIZE = 64;

    @TempDir
    Path tempDir;

    @Test
    void runBenchmark() throws Exception {
        List<Integer> sizes = List.of(10_000, 50_000, 100_000);
        List<ResultRow> rows = new ArrayList<>();

        for (int size : sizes) {
            List<Record> data = generate(size);

            // RandomAccessFile
            Path rafPath = tempDir.resolve("raf-" + size + ".bin");
            rows.add(runForBackend("RandomAccessFile", size, data,
                    () -> {
                        writeRandomAccessFile(rafPath, data);
                        return 0L;
                    },
                    () -> readSequentialRandomAccessFile(rafPath, size),
                    () -> randomReadRandomAccessFile(rafPath, size, 5_000)));

            // FileChannel + ByteBuffer
            Path channelPath = tempDir.resolve("channel-" + size + ".bin");
            rows.add(runForBackend("FileChannel+ByteBuffer", size, data,
                    () -> {
                        writeFileChannel(channelPath, data);
                        return 0L;
                    },
                    () -> readSequentialFileChannel(channelPath, size),
                    () -> 0L)); // random access не принципиален для канала в этом сценарии

            // Memory-mapped
            Path mmapPath = tempDir.resolve("mmap-" + size + ".bin");
            rows.add(runForBackend("MemoryMapped", size, data,
                    () -> {
                        writeMemoryMapped(mmapPath, data);
                        return 0L;
                    },
                    () -> readSequentialMemoryMapped(mmapPath, size),
                    () -> randomReadMemoryMapped(mmapPath, size, 5_000)));
        }

        printTable(rows);

        // Best-effort очистка временных файлов, чтобы не оставлять mmap под Windows
        System.gc();
        try {
            java.nio.file.Files.list(tempDir).forEach(p -> {
                try {
                    java.nio.file.Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
            // noop
        }
    }

    private ResultRow runForBackend(String backend,
                                    int size,
                                    List<Record> data,
                                    MeasuredAction write,
                                    MeasuredAction seqRead,
                                    MeasuredAction randomRead) throws Exception {
        long memBefore = usedMemory();
        long writeMs = measure(write);
        long seqReadMs = measure(seqRead);
        long randomReadMs = measure(randomRead);
        long memAfter = usedMemory();
        long memPeak = Math.max(memBefore, memAfter);
        return new ResultRow(backend, size, writeMs, seqReadMs, randomReadMs, memPeak);
    }

    private long measure(MeasuredAction action) throws Exception {
        long start = System.nanoTime();
        long checksum = action.run();
        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        // Небольшой вывод контрольной суммы, чтобы не выкидывался код
        if (checksum == Long.MIN_VALUE) {
            throw new IllegalStateException("checksum guard");
        }
        return durationMs;
    }

    private void writeRandomAccessFile(Path path, List<Record> data) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw")) {
            raf.setLength(0);
            byte[] buf = new byte[RECORD_SIZE];
            ByteBuffer buffer = ByteBuffer.wrap(buf);
            for (Record record : data) {
                buffer.clear();
                writeRecord(buffer, record);
                raf.write(buf);
            }
            raf.getFD().sync();
        }
    }

    private long readSequentialRandomAccessFile(Path path, int count) throws IOException {
        long checksum = 0;
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] buf = new byte[RECORD_SIZE];
            for (int i = 0; i < count; i++) {
                int read = raf.read(buf);
                if (read < RECORD_SIZE) {
                    break;
                }
                ByteBuffer buffer = ByteBuffer.wrap(buf);
                checksum += buffer.getLong();
            }
        }
        return checksum;
    }

    private long randomReadRandomAccessFile(Path path, int count, int iterations) throws IOException {
        long checksum = 0;
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] buf = new byte[RECORD_SIZE];
            for (int i = 0; i < iterations; i++) {
                int index = ThreadLocalRandom.current().nextInt(count);
                raf.seek((long) index * RECORD_SIZE);
                int read = raf.read(buf);
                if (read < RECORD_SIZE) {
                    continue;
                }
                ByteBuffer buffer = ByteBuffer.wrap(buf);
                checksum ^= buffer.getLong(0);
            }
        }
        return checksum;
    }

    private void writeFileChannel(Path path, List<Record> data) throws IOException {
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(RECORD_SIZE * 1024);
            for (Record record : data) {
                if (buffer.remaining() < RECORD_SIZE) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                    buffer.clear();
                }
                writeRecord(buffer, record);
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
    }

    private long readSequentialFileChannel(Path path, int count) throws IOException {
        long checksum = 0;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(RECORD_SIZE * 1024);
            int read;
            while ((read = channel.read(buffer)) != -1) {
                if (read == 0) {
                    continue;
                }
                buffer.flip();
                while (buffer.remaining() >= RECORD_SIZE) {
                    checksum += buffer.getLong();
                    buffer.position(buffer.position() + RECORD_SIZE - Long.BYTES);
                }
                buffer.compact();
            }
        }
        return checksum;
    }

    private void writeMemoryMapped(Path path, List<Record> data) throws IOException {
        long fileSize = (long) data.size() * RECORD_SIZE;
        try (FileChannel channel = FileChannel.open(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.READ)) {
            java.nio.MappedByteBuffer mapped = channel.map(MapMode.READ_WRITE, 0, fileSize);
            for (Record record : data) {
                writeRecord(mapped, record);
            }
            mapped.force();
            cleanMapped(mapped);
        }
    }

    private long readSequentialMemoryMapped(Path path, int count) throws IOException {
        long checksum = 0;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            java.nio.MappedByteBuffer mapped = channel.map(MapMode.READ_ONLY, 0, channel.size());
            for (int i = 0; i < count; i++) {
                checksum += mapped.getLong();
                mapped.position(mapped.position() + RECORD_SIZE - Long.BYTES);
            }
            cleanMapped(mapped);
        }
        return checksum;
    }

    private long randomReadMemoryMapped(Path path, int count, int iterations) throws IOException {
        long checksum = 0;
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            java.nio.MappedByteBuffer mapped = channel.map(MapMode.READ_ONLY, 0, channel.size());
            for (int i = 0; i < iterations; i++) {
                int index = ThreadLocalRandom.current().nextInt(count);
                int pos = index * RECORD_SIZE;
                long value = mapped.getLong(pos);
                checksum ^= value;
            }
            cleanMapped(mapped);
        }
        return checksum;
    }

    private List<Record> generate(int count) {
        List<Record> data = new ArrayList<>(count);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            data.add(new Record(
                    i,
                    System.currentTimeMillis(),
                    random.nextInt(50_000, 250_000),
                    random.nextLong(),
                    random.nextLong(),
                    random.nextLong(),
                    random.nextLong(),
                    random.nextInt(),
                    random.nextInt(),
                    random.nextInt()
            ));
        }
        return data;
    }

    private void writeRecord(ByteBuffer buffer, Record record) {
        buffer.putLong(record.id);
        buffer.putLong(record.timestamp);
        buffer.putInt(record.salary);
        buffer.putLong(record.companyHash);
        buffer.putLong(record.cityHash);
        buffer.putLong(record.score);
        buffer.putLong(record.offset);
        buffer.putInt(record.pad1);
        buffer.putInt(record.pad2);
        buffer.putInt(record.pad3);
    }

    @SuppressWarnings("removal")
    private void cleanMapped(java.nio.MappedByteBuffer buffer) {
        try {
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);
            if (cleaner != null) {
                Method clean = cleaner.getClass().getMethod("clean");
                clean.invoke(cleaner);
            }
        } catch (Exception ignored) {
            // Best-effort unmapping; if it fails, temp dir cleanup may still succeed after GC.
        }
    }

    private long usedMemory() {
        Runtime r = Runtime.getRuntime();
        return r.totalMemory() - r.freeMemory();
    }

    private void printTable(List<ResultRow> rows) {
        System.out.println("=== File I/O benchmark (64-byte binary records) ===");
        System.out.printf(Locale.ROOT, "%-24s %8s %12s %15s %15s %12s%n",
                "Backend", "Records", "Write ms", "Seq read ms", "Random read ms", "Used MB");
        for (ResultRow row : rows) {
            System.out.printf(Locale.ROOT, "%-24s %8d %12d %15d %15d %12.2f%n",
                    row.backend, row.count, row.writeMs, row.seqReadMs, row.randomReadMs,
                    row.usedMemoryBytes / 1024.0 / 1024.0);
        }
    }

    private record ResultRow(String backend,
                             int count,
                             long writeMs,
                             long seqReadMs,
                             long randomReadMs,
                             long usedMemoryBytes) {
    }

    private record Record(long id,
                          long timestamp,
                          int salary,
                          long companyHash,
                          long cityHash,
                          long score,
                          long offset,
                          int pad1,
                          int pad2,
                          int pad3) {
    }

    @FunctionalInterface
    private interface MeasuredAction {
        long run() throws Exception;
    }
}
