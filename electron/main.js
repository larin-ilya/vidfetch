'use strict';

const { app, BrowserWindow, ipcMain, dialog, shell, clipboard } = require('electron');
const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

let mainWindow = null;
let engine = null;
let engineReady = false;

const pending = new Map(); // id -> {resolve, reject}
let nextId = 1;
let stdoutBuf = '';
let stderrBuf = '';

// ---------------------------------------------------------------------------
// Settings persistence
// ---------------------------------------------------------------------------

const settingsPath = () => path.join(app.getPath('userData'), 'settings.json');

const DEFAULT_SETTINGS = {
  downloadDir: app.getPath('downloads'),
  maxConcurrent: 2,
  speedLimit: 0, // 0 = unlimited, else bytes/sec
  clipboardWatch: false,
  afterDownload: 'nothing', // nothing | openFolder | shutdown
  theme: 'system', // light | dark | system
  language: 'ru', // en | ru
};

function loadSettings() {
  try {
    const raw = fs.readFileSync(settingsPath(), 'utf8');
    return Object.assign({}, DEFAULT_SETTINGS, JSON.parse(raw));
  } catch (e) {
    return Object.assign({}, DEFAULT_SETTINGS);
  }
}

function saveSettings(s) {
  try {
    fs.mkdirSync(app.getPath('userData'), { recursive: true });
    fs.writeFileSync(settingsPath(), JSON.stringify(s, null, 2), 'utf8');
  } catch (e) {
    console.error('saveSettings failed', e);
  }
}

let settings = loadSettings();

// ---------------------------------------------------------------------------
// Engine lifecycle
// ---------------------------------------------------------------------------

function resolveEngine() {
  if (app.isPackaged) {
    const exe = path.join(
      process.resourcesPath, 'engine',
      process.platform === 'win32' ? 'engine.exe' : 'engine'
    );
    return { cmd: exe, args: [] };
  }
  const py = process.platform === 'win32' ? 'python' : 'python3';
  const script = path.join(app.getAppPath(), 'engine', 'engine.py');
  return { cmd: py, args: [script] };
}

function ffmpegDir() {
  if (app.isPackaged) {
    return path.join(process.resourcesPath, 'ffmpeg');
  }
  return path.join(app.getAppPath(), 'build', 'ffmpeg');
}

function startEngine() {
  const { cmd, args } = resolveEngine();
  const env = Object.assign({}, process.env, { VIDFETCH_FFMPEG_DIR: ffmpegDir() });
  const child = spawn(cmd, args, { env, stdio: ['pipe', 'pipe', 'pipe'] });
  engine = child;

  child.stdout.setEncoding('utf8');
  child.stdout.on('data', (chunk) => {
    stdoutBuf += chunk;
    let idx;
    while ((idx = stdoutBuf.indexOf('\n')) >= 0) {
      const line = stdoutBuf.slice(0, idx).trim();
      stdoutBuf = stdoutBuf.slice(idx + 1);
      if (line) handleEngineLine(line);
    }
  });

  child.stderr.setEncoding('utf8');
  child.stderr.on('data', (chunk) => {
    stderrBuf += chunk;
    if (stderrBuf.length > 8000) stderrBuf = stderrBuf.slice(-8000);
  });

  child.on('exit', (code) => {
    engineReady = false;
    engine = null;
    const err = new Error(`engine exited (code ${code})`);
    for (const [, p] of pending) p.reject(err);
    pending.clear();
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('engine:status', { alive: false, stderr: stderrBuf.slice(-2000) });
    }
  });

  child.on('error', (e) => {
    engineReady = false;
    for (const [, p] of pending) p.reject(e);
    pending.clear();
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('engine:status', { alive: false, stderr: String(e) });
    }
  });
}

function handleEngineLine(line) {
  let msg;
  try {
    msg = JSON.parse(line);
  } catch (e) {
    return;
  }
  if (msg.event === 'ready') {
    engineReady = true;
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('engine:status', { alive: true, data: msg.data });
    }
    return;
  }
  if (msg.event) {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('engine:event', msg);
    }
    return;
  }
  if (msg.id != null && pending.has(msg.id)) {
    const p = pending.get(msg.id);
    pending.delete(msg.id);
    if (msg.ok) p.resolve(msg.result);
    else p.reject(new Error(msg.error || 'engine error'));
  }
}

function callEngine(method, params) {
  return new Promise((resolve, reject) => {
    if (!engine || !engineReady) {
      reject(new Error('engine not ready'));
      return;
    }
    const id = nextId++;
    pending.set(id, { resolve, reject });
    try {
      engine.stdin.write(JSON.stringify({ id, method, params: params || {} }) + '\n');
    } catch (e) {
      pending.delete(id);
      reject(e);
    }
  });
}

// ---------------------------------------------------------------------------
// Window
// ---------------------------------------------------------------------------

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1080,
    height: 720,
    minWidth: 760,
    minHeight: 540,
    frame: false,
    backgroundColor: '#f4f5f7',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  mainWindow.loadFile(path.join(__dirname, '..', 'renderer', 'index.html'));

  mainWindow.once('ready-to-show', () => mainWindow.show());

  mainWindow.on('maximize', () => sendWin('maximized', true));
  mainWindow.on('unmaximize', () => sendWin('maximized', false));

  mainWindow.on('closed', () => { mainWindow = null; });
}

function sendWin(channel, data) {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.webContents.send(channel, data);
  }
}

// ---------------------------------------------------------------------------
// IPC
// ---------------------------------------------------------------------------

function registerIpc() {
  ipcMain.handle('engine:call', (_e, method, params) => callEngine(method, params));
  ipcMain.handle('engine:restart', () => { if (engine) engine.kill(); startEngine(); return true; });

  ipcMain.on('win:minimize', () => mainWindow && mainWindow.minimize());
  ipcMain.on('win:maximize', () => {
    if (!mainWindow) return;
    if (mainWindow.isMaximized()) mainWindow.unmaximize();
    else mainWindow.maximize();
  });
  ipcMain.on('win:close', () => mainWindow && mainWindow.close());
  ipcMain.handle('win:isMaximized', () => !!(mainWindow && mainWindow.isMaximized()));

  ipcMain.handle('fs:openPath', (_e, p) => { shell.openPath(p); return true; });
  ipcMain.handle('fs:reveal', (_e, p) => { shell.showItemInFolder(p); return true; });

  ipcMain.handle('dialog:pickFolder', async (_e, defaultPath) => {
    const r = await dialog.showOpenDialog(mainWindow, {
      properties: ['openDirectory', 'createDirectory'],
      defaultPath: defaultPath || settings.downloadDir,
    });
    return r.canceled ? null : r.filePaths[0];
  });

  ipcMain.handle('settings:get', () => settings);
  ipcMain.handle('settings:set', (_e, patch) => {
    settings = Object.assign({}, settings, patch || {});
    saveSettings(settings);
    return settings;
  });

  ipcMain.handle('clipboard:read', () => clipboard.readText());
}

// ---------------------------------------------------------------------------
// App lifecycle
// ---------------------------------------------------------------------------

app.whenReady().then(() => {
  // Dev screenshot mode: electron . --screenshot=<path> [--shot-fill=<url>]
  const shotIdx = process.argv.findIndex((a) => a.startsWith('--screenshot='));
  if (shotIdx >= 0) {
    const shotPath = process.argv[shotIdx].split('=')[1];
    const fillIdx = process.argv.findIndex((a) => a.startsWith('--shot-fill='));
    const fillUrl = fillIdx >= 0 ? process.argv[fillIdx].split('=').slice(1).join('=') : null;
    registerIpc();
    createWindow();
    startEngine();
    mainWindow.webContents.once('did-finish-load', () => {
      setTimeout(async () => {
        try {
          if (fillUrl) {
            await mainWindow.webContents.executeJavaScript(
              `window.__vf.setUrl(${JSON.stringify(fillUrl)}); window.__vf.analyze(${JSON.stringify(fillUrl)});`
            );
            await new Promise((r) => setTimeout(r, 6000));
          }
          const img = await mainWindow.webContents.capturePage();
          fs.writeFileSync(shotPath, img.toPNG());
          console.log('SHOT_SAVED', shotPath);
        } catch (e) {
          console.error('SHOT_ERR', e);
        }
        app.quit();
      }, 1500);
    });
    return;
  }

  registerIpc();
  createWindow();
  startEngine();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', () => {
  if (engine) {
    try { engine.stdin.end(); } catch (e) {}
    try { engine.kill(); } catch (e) {}
  }
});
