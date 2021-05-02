package io.github.makingthematrix;

import io.vavr.Lazy;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

final class ManifestToFix {
    private static final String AUTOMATIC_MODULE_NAME = "Automatic-Module-Name";
    private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";

    private static final Lazy<ZipParameters> zipParams = Lazy.of(() -> {
        final var params = new ZipParameters();
        params.setIncludeRootFolder(true);
        params.setOverrideExistingFilesInZip(true);
        params.setFileNameInZip(MANIFEST_MF);
        return params;
    });

    private final File manifestFile;
    private final ZipFile zipFile;

    public ManifestToFix(@NotNull final File artifactFile, @NotNull final File outputDir) throws IOException {
        this.zipFile = new ZipFile(artifactFile);
        zipFile.extractFile(MANIFEST_MF, outputDir.getAbsolutePath());
        this.manifestFile = new File(outputDir, MANIFEST_MF);
    }

    public boolean fixLib(@NotNull String newLibraryName) throws IOException {
        if (manifestFile.exists()) {
            return fixManifestFile(newLibraryName);
        } else {
            throw new IOException("No file " + manifestFile.getAbsolutePath() + " found");
        }
    }

    private boolean fixManifestFile(String newLibraryName) throws IOException {
        final var manifestLines = FileUtils.readLines(manifestFile, Charset.defaultCharset());
        final var moduleNameLine =
            manifestLines.stream()
                .filter(line -> line.toLowerCase().contains(AUTOMATIC_MODULE_NAME.toLowerCase()))
                .findAny();

        if (moduleNameLine.isPresent()) {
            return false;
        }

        addAutomaticModuleName(manifestLines, newLibraryName);
        if (!zipFile.isValidZipFile()) {
            throw new IOException("After the operation the zip file is INVALID: " + zipFile.getFile().getAbsolutePath());
        }
        return true;
    }

    private void addAutomaticModuleName(List<String> manifestLines, String newLibraryName) throws IOException {
        final var newManifestLines =
            List.copyOf(manifestLines).stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());
        newManifestLines.add(AUTOMATIC_MODULE_NAME + ": " + newLibraryName);

        manifestFile.createNewFile();
        FileUtils.writeLines(manifestFile, newManifestLines);
        zipFile.addFile(manifestFile, zipParams.get());
    }
}
