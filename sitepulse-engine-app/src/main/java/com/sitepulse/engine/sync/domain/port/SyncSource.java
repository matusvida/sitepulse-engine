package com.sitepulse.engine.sync.domain.port;

import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import java.util.List;

public interface SyncSource {

    List<String> listSubfolders(String sourcePath);

    List<SourceImageFile> listFiles(String sourcePath, String subfolderName);

    byte[] downloadFile(String sourcePath, String relativePath);
}
