/**
 * Android WebView Bridge - replaces Wails runtime bindings
 * 
 * This module provides the same API as the original Wails `wailsjs/go/main/App`
 * but communicates with Kotlin via Android's @JavascriptInterface.
 */

// Declare the Android interface injected by Kotlin WebView
declare global {
  interface Window {
    Android?: {
      selectFiles: () => void;
      selectFolder: () => void;
      selectFilesFromFolder: (ext: string) => void;
      processFiles: (filesJson: string, savePath: string) => void;
      loadConfig: () => string;
      saveConfig: (configJson: string) => void;
    };
    // Callbacks called from Kotlin
    onFilesSelected?: (filesJson: string) => void;
    onFolderSelected?: (path: string) => void;
    onFolderFilesSelected?: (filesJson: string) => void;
    onFileStatusChanged?: (index: number, status: string) => void;
    onConfigLoaded?: (configJson: string) => void;
  }
}

// ---- Promise-based wrappers matching Wails API ----

let _resolveSelectFiles: ((files: string[]) => void) | null = null;
let _resolveSelectFolder: ((path: string) => void) | null = null;
let _resolveSelectFilesFromFolder: ((files: string[]) => void) | null = null;

// Called from Kotlin when files are selected
window.onFilesSelected = (filesJson: string) => {
  if (_resolveSelectFiles) {
    try {
      const files = JSON.parse(filesJson) as string[];
      _resolveSelectFiles(files);
    } catch {
      _resolveSelectFiles([]);
    }
    _resolveSelectFiles = null;
  }
};

// Called from Kotlin when folder is selected
window.onFolderSelected = (path: string) => {
  if (_resolveSelectFolder) {
    _resolveSelectFolder(path);
    _resolveSelectFolder = null;
  }
};

// Called from Kotlin when files from folder are selected
window.onFolderFilesSelected = (filesJson: string) => {
  if (_resolveSelectFilesFromFolder) {
    try {
      const files = JSON.parse(filesJson) as string[];
      _resolveSelectFilesFromFolder(files);
    } catch {
      _resolveSelectFilesFromFolder([]);
    }
    _resolveSelectFilesFromFolder = null;
  }
};

/**
 * SelectFiles - opens Android file picker for .ncm files
 * Replaces: wailsjs/go/main/App.SelectFiles
 */
export function SelectFiles(): Promise<string[]> {
  return new Promise((resolve) => {
    _resolveSelectFiles = resolve;
    if (window.Android) {
      window.Android.selectFiles();
    } else {
      console.warn('Android bridge not available');
      resolve([]);
    }
  });
}

/**
 * SelectFolder - opens Android folder picker
 * Replaces: wailsjs/go/main/App.SelectFolder
 */
export function SelectFolder(): Promise<string> {
  return new Promise((resolve) => {
    _resolveSelectFolder = resolve;
    if (window.Android) {
      window.Android.selectFolder();
    } else {
      console.warn('Android bridge not available');
      resolve('');
    }
  });
}

/**
 * SelectFilesFromFolder - opens folder picker and lists .ncm files within
 * Replaces: wailsjs/go/main/App.SelectFilesFromFolder
 */
export function SelectFilesFromFolder(ext: string): Promise<string[]> {
  return new Promise((resolve) => {
    _resolveSelectFilesFromFolder = resolve;
    if (window.Android) {
      window.Android.selectFilesFromFolder(ext);
    } else {
      console.warn('Android bridge not available');
      resolve([]);
    }
  });
}

// NcmFile type matching Go's main.NcmFile
export interface NcmFile {
  Name: string;
  Status: string;
}

/**
 * ProcessFiles - starts processing selected NCM files
 * Replaces: wailsjs/go/main/App.ProcessFiles
 */
export function ProcessFiles(files: NcmFile[], savePath: string): Promise<void> {
  return new Promise((resolve) => {
    if (window.Android) {
      window.Android.processFiles(JSON.stringify(files), savePath);
    } else {
      console.warn('Android bridge not available');
    }
    resolve();
  });
}

// ---- Config Manager ----

export interface ConfigData {
  save_to: string;
  path: string;
}

/**
 * Load - loads saved config
 * Replaces: wailsjs/go/utils/ConfigManager.Load
 */
export function Load(): Promise<ConfigData> {
  return new Promise((resolve) => {
    if (window.Android) {
      try {
        const configStr = window.Android.loadConfig();
        const config = JSON.parse(configStr) as ConfigData;
        resolve(config);
      } catch {
        resolve({ save_to: 'original', path: '' });
      }
    } else {
      resolve({ save_to: 'original', path: '' });
    }
  });
}

/**
 * Save - saves config
 * Replaces: wailsjs/go/utils/ConfigManager.Save
 */
export function Save(config: ConfigData): Promise<void> {
  return new Promise((resolve) => {
    if (window.Android) {
      window.Android.saveConfig(JSON.stringify(config));
    }
    resolve();
  });
}

// ---- Event System ----

type EventCallback = (...args: any[]) => void;
const eventListeners: Record<string, EventCallback[]> = {};

/**
 * EventsOn - subscribe to events from Kotlin backend
 * Replaces: wailsjs/runtime/runtime.EventsOn
 */
export function EventsOn(eventName: string, callback: EventCallback): void {
  if (!eventListeners[eventName]) {
    eventListeners[eventName] = [];
  }
  eventListeners[eventName].push(callback);
}

// Global handler that Kotlin calls to emit events
window.onFileStatusChanged = (index: number, status: string) => {
  const callbacks = eventListeners['file-status-changed'];
  if (callbacks) {
    callbacks.forEach(cb => cb(index, status));
  }
};

/**
 * OnFileDrop - file drop handler (no-op on Android, uses file picker instead)
 * Replaces: wailsjs/runtime/runtime.OnFileDrop
 */
export function OnFileDrop(_callback: (...args: any[]) => void, _disabled: boolean): void {
  // File drop is not supported on Android WebView
  // Users will use the file picker button instead
}
