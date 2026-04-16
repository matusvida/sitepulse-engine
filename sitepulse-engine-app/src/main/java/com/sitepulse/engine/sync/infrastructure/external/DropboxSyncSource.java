package com.sitepulse.engine.sync.infrastructure.external;
import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import com.sitepulse.engine.sync.domain.port.SyncSource;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DropboxSyncSource implements SyncSource {

    private final DropboxClientService dropboxClientService;

    @Override
    public List<String> listSubfolders(String sourcePath) {
        return dropboxClientService.listSubfolders(sourcePath);
    }

    @Override
    public List<SourceImageFile> listFiles(String sourcePath, String subfolderName) {
        return dropboxClientService.listFiles(sourcePath, subfolderName).stream()
                .map(file -> new SourceImageFile(file.name(), file.path(), file.size()))
                .toList();
    }

    @Override
    public InputStream downloadFileStream(String sourcePath, String relativePath) {
        return dropboxClientService.downloadFileStream(sourcePath, relativePath);
    }

    @Override
    public byte[] downloadFile(String sourcePath, String relativePath) {
        return dropboxClientService.downloadFile(sourcePath, relativePath);
    }
}
