package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import java.io.InputStream;
import java.util.List;

public interface SyncSource {

    List<String> listSubfolders(String sourcePath);

    List<SourceImageFile> listFiles(String sourcePath, String subfolderName);

    InputStream downloadFileStream(String sourcePath, String relativePath);

    byte[] downloadFile(String sourcePath, String relativePath);
}
