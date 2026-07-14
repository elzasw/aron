package cz.aron.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileManagerService {
	
    private static final Random rnd = new Random();
    
    private final AtomicLong counter = new AtomicLong();
	
	@Value("${files.storage}")
	private String rootPath;
	
	private int parentCount = 2;
	private int parentLenght = 2;
	
	public String storeFile(InputStream is, String uuid) throws IOException {
		String handle = createHandle();
		Path path = Paths.get(rootPath).resolve(handle);
		Files.createDirectories(path.getParent());
		Files.copy(is, path);
		return handle;
	}
	
	private String createHandle() {		
		StringBuilder sb = new StringBuilder();		
		for (int c = 0; c < parentCount; c++) {            
            for (int l = 0; l < parentLenght; l++) {
                int n = rnd.nextInt(36);
                n += n < 10 ? 48 : 87; // 0-9 and a-z
                sb.append((char) n);
            }
            sb.append("/");
        }		
		sb.append(""+System.currentTimeMillis()+"_"+counter.incrementAndGet());
		return sb.toString();		
	}

}
