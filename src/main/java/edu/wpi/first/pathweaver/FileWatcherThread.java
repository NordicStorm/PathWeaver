package edu.wpi.first.pathweaver;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class FileWatcherThread extends Thread{
	
	FileWatcherThread(){
		instance=this;
		try {
			this.watcher = FileSystems.getDefault().newWatchService();
		} catch (IOException e) {
			e.printStackTrace();
		}
        this.keys = new HashMap<WatchKey,Path>();
        this.callbacks = new HashMap<>();

        
	}
	private WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final Map<String,Runnable> callbacks;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    public void registerDirectory(Path dir) {
        try {
			WatchKey key = dir.register(watcher, ENTRY_MODIFY);
	        keys.put(key, dir);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
    }
    
    /**
     * Filename is the full file path
     * @param filename
     * @param callback
     */
    public void registerFileCallback(String filename, Runnable callback) {
        callbacks.put(filename, callback);
    }
    public void clearAllWatchedDirsAndFiles() {
    	for(WatchKey key : keys.keySet()) {
    		key.cancel();
    	}
    	keys.clear();
    	callbacks.clear();
    }
	@Override
	public void run() {
		for (;;) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);
                Runnable callback = callbacks.get(child.toString());
                if (callback == null) {
                    continue;
                }
                callback.run();
                
                // print out event
                //System.out.format("%s: %s\n", event.kind().name(), child);

            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

            }
        }
	}
	private static FileWatcherThread instance;
	public static FileWatcherThread getInstance() {
		return instance;
	}
}
