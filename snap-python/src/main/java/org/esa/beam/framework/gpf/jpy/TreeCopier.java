package org.esa.beam.framework.gpf.jpy;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A {@code FileVisitor} that copies a file tree.
 */
class TreeCopier implements FileVisitor<Path> {
    private final Path source;
    private final Path target;
    private IOException exception;

    static Path copy(Path source, Path target) throws IOException {
        if (!Files.exists(target)) {
            target = Files.createDirectories(target);
        }
        Path dir = target.resolve(source.getFileName());
        TreeCopier treeCopier = new TreeCopier(source, dir);
        Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, treeCopier);
        if (treeCopier.exception != null) {
            throw treeCopier.exception;
        }
        return dir;
    }

    TreeCopier(Path source, Path target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        Files.copy(file, target.resolve(source.relativize(file)), COPY_ATTRIBUTES, REPLACE_EXISTING);
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
        try {
            Files.copy(dir, target.resolve(source.relativize(dir)), COPY_ATTRIBUTES);
        } catch (FileAlreadyExistsException x) {
            // ignore
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
        if (exception == null) {
            Path newDir = target.resolve(source.relativize(dir));
            FileTime time = Files.getLastModifiedTime(dir);
            Files.setLastModifiedTime(newDir, time);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exception) {
        this.exception = exception;
        return TERMINATE;
    }
}
