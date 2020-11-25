package cz.inqool.eas.common.storage.file;

import cz.inqool.eas.common.domain.Domain;
import cz.inqool.eas.common.domain.DomainService;
import cz.inqool.eas.common.exception.*;
import lombok.Builder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.function.Function;

import static cz.inqool.eas.common.exception.ExceptionUtils.checked;
import static cz.inqool.eas.common.exception.ForbiddenOperation.ErrorCode.WRONG_STATE;
import static cz.inqool.eas.common.exception.InvalidArgument.ErrorCode.SIZE_TOO_BIG;
import static cz.inqool.eas.common.exception.MissingAttribute.ErrorCode.FIELD_IS_NULL;
import static cz.inqool.eas.common.utils.AssertionUtils.*;
import static java.nio.file.Files.*;

@SuppressWarnings("JavadocReference")
@Builder
@Slf4j
public class FileManager {
    private static final int DIR_NAME_LENGTH = 2;

    private FileStore store;

    @Setter
    private String directoryPath;

    @Setter
    private Long fileSizeLimit;

    @Setter
    private int hierarchicalLevel;

    /**
     * Stores a new file to the file system storage temporarily from provided Multipart.
     *
     * @param multipart Multipart to store
     * @return created file entity - {@link File}
     * @throws InvalidArgument  if the file size exceeds allowed size limit
     * @throws GeneralException if any other I/O exception occurs
     */
    @Transactional
    public File upload(@Nonnull MultipartFile multipart) {
        lte(multipart.getSize(), fileSizeLimit, () -> new InvalidArgument(multipart.getName(), SIZE_TOO_BIG));

        String name = getFileName(multipart);

        File file = new File();
        file.setName(name);
        file.setContentType(multipart.getContentType());
        file.setSize(multipart.getSize());
        file.setLevel(hierarchicalLevel);
        file.setPermanent(false);

        checked(() -> {
            Path path = getPath(file.getId());
            createDirectories(path.getParent());
            copy(multipart.getInputStream(), path);
        });

        return store.create(file);
    }

    /**
     * Store a new file to the file system storage permanently.
     *
     * @param name name of the file
     * @param size size of the file
     * @param contentType content type
     * @return created file entity - {@link File}
     * @throws InvalidArgument  if the file size exceeds allowed size limit
     * @throws GeneralException if any other I/O exception occurs
     */
    @Transactional
    public File store(String name, long size, String contentType, InputStream stream) {
        lte(size, fileSizeLimit, () -> new InvalidArgument(name, SIZE_TOO_BIG));

        File file = new File();
        file.setName(name);
        file.setContentType(contentType);
        file.setSize(size);
        file.setLevel(hierarchicalLevel);
        file.setPermanent(true);

        checked(() -> {
            Path path = getPath(file.getId());
            createDirectories(path.getParent());
            copy(stream, path);
        });

        return store.create(file);
    }


    @Transactional
    public File storeFromUpload(File file) {
        eq(file.permanent, false, () -> new ForbiddenOperation(file, WRONG_STATE));

        file.setPermanent(true);
        return store.update(file);
    }

    /**
     * Deletes a file.
     *
     * @param id Id of file to be deleted
     * @return deleted file entity - {@link File}
     * @throws GeneralException if any other I/O exception occurs
     */
    @Transactional
    public File remove(String id) {
        File file = store.delete(id);
        notNull(file, () -> new MissingObject(File.class, id));

        Path path = getPath(file.getId());

        if (exists(path)) {
            checked(() -> Files.delete(path));
        } else {
            log.warn("File {} not found.", path);
        }
        return file;
    }

    @Transactional
    public File discardUpload(@Nonnull String id) {
        File file = store.delete(id);
        notNull(file, () -> new MissingObject(File.class, id));
        eq(file.permanent, false, () -> new ForbiddenOperation(file, WRONG_STATE));

        Path path = getPath(file.getId());

        if (exists(path)) {
            checked(() -> Files.delete(path));
        } else {
            log.warn("File {} not found.", path);
        }
        return file;
    }

    /**
     * Open specified file.
     *
     * @return initialized input stream ready to be read from with file descriptor
     */
    @Transactional
    public OpenedFile open(String id) {
        File file = store.find(id);
        notNull(file, () -> new MissingObject(File.class, id));

        Path path = getPath(file.getId());

        InputStream stream = checked(() -> newInputStream(path, StandardOpenOption.READ));

        return new OpenedFile(file, stream);
    }

    /**
     * Open specified file.
     *
     * @return initialized input stream ready to be read from with file descriptor
     */
    @Transactional
    public File get(String id) {
        File file = store.find(id);
        notNull(file, () -> new MissingObject(File.class, id));

        return file;
    }

    /**
     * Compute the path to actual file with given directory hierarchy.
     *
     * @param id id of the file
     * @return computed path
     */
    private Path getPath(String id) {
        String[] path = new String[hierarchicalLevel + 1];
        path[hierarchicalLevel] = id;

        String uuid = id.replaceAll("-", "");
        for (int i = 0; i < hierarchicalLevel; i++) {
            path[i] = uuid.substring(i * DIR_NAME_LENGTH, i * DIR_NAME_LENGTH + DIR_NAME_LENGTH);
        }

        return Paths.get(directoryPath, path);
    }

    /**
     * Returns the actual filename of given multipart file.
     *
     * @throws MissingAttribute if file name is not available
     */
    private String getFileName(MultipartFile file) {
        String filename = file.getOriginalFilename();
        notEmpty(filename, () -> new MissingAttribute(MultipartFile.class, file.getName(), "filename", FIELD_IS_NULL));

        return FilenameUtils.getName(filename);
    }

    @Autowired
    public void setStore(FileStore store) {
        this.store = store;
    }

    /**
     * Permanently saves uploaded file for object.
     *
     * Should be called in {@link DomainService#preCreateHook(Domain)}
     *
     * @param object Object
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preCreateHook(ENTITY object, Function<ENTITY, File> mapper) {
        File file = mapper.apply(object);

        if (file != null) {
            File content = this.get(file.getId());
            notNull(content, () -> new MissingObject(File.class, file.getId()));

            if (!content.isPermanent()) {
                log.debug("Storing '{}'.", file);
                this.storeFromUpload(content);
            }
        }
    }

    /**
     * Permanently saves uploaded files for object.
     *
     * Should be called in {@link DomainService#preCreateHook(Domain)}
     *
     * @param object Object
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preCreateHookCollection(ENTITY object, Function<ENTITY, Collection<File>> mapper) {
        Collection<File> files = mapper.apply(object);

        if (files != null) {
            files.forEach(file -> {
                File content = this.get(file.getId());
                notNull(content, () -> new MissingObject(File.class, file.getId()));

                if (!content.isPermanent()) {
                    log.debug("Storing '{}'.", file);
                    this.storeFromUpload(content);
                }
            });
        }
    }

    /**
     * Permanently saves uploaded file for object and removed unused.
     *
     * Should be called in {@link DomainService#preUpdateHook(Domain)}
     *
     * @param object Object
     * @param old Old instance
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preUpdateHook(ENTITY object, ENTITY old, Function<ENTITY, File> mapper) {
        File file = mapper.apply(object);

        if (file != null) {
            File content = this.get(file.getId());
            notNull(content, () -> new MissingObject(File.class, file.getId()));

            if (!content.isPermanent()) {
                log.debug("Storing '{}'.", file);
                this.storeFromUpload(content);
            }
        }

        File oldFile = mapper.apply(old);
        if (oldFile != null) {
            if (!oldFile.equals(file)) {
                log.debug("Removing unused '{}'.", oldFile);
                this.remove(oldFile.getId());
            }
        }
    }

    /**
     * Permanently saves uploaded file for object and removes unused.
     *
     * Should be called in {@link DomainService#preUpdateHook(Domain)}
     *
     * @param object Object
     * @param old Old instance
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preUpdateHookCollection(ENTITY object, ENTITY old, Function<ENTITY, Collection<File>> mapper) {
        Collection<File> files = mapper.apply(object);

        if (files != null) {
            files.forEach(file -> {
                File content = this.get(file.getId());
                notNull(content, () -> new MissingObject(File.class, file.getId()));

                if (!content.isPermanent()) {
                    log.debug("Storing '{}'.", file);
                    this.storeFromUpload(content);
                }
            });
        }

        Collection<File> oldFiles = mapper.apply(old);
        if (oldFiles != null) {
            oldFiles.forEach(oldFile -> {
                if (files != null && !files.contains(oldFile)) {
                    log.debug("Removing unused '{}'.", oldFile);
                    this.remove(oldFile.getId());
                }
            });
        }
    }

    /**
     * Deletes saved file for object.
     *
     * Should be called in {@link DomainService#preDeleteHook(String)}}
     *
     * @param object Object
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preDeleteHook(ENTITY object, Function<ENTITY, File> mapper) {
        File file = mapper.apply(object);

        if (file != null) {
            log.debug("Removing unused '{}'.", file);
            this.remove(file.getId());
        }
    }

    /**
     * Deletes saved file for object.
     *
     * Should be called in {@link DomainService#preDeleteHook(String)}}
     *
     * @param object Object
     * @param mapper Mapper for File attribute
     * @param <ENTITY> Entity Type
     */
    public <ENTITY> void preDeleteHookCollection(ENTITY object, Function<ENTITY, Collection<File>> mapper) {
        Collection<File> files = mapper.apply(object);

        if (files != null) {
            files.forEach(file -> {
                log.debug("Removing unused '{}'.", file);
                this.remove(file.getId());
            });
        }
    }
}
