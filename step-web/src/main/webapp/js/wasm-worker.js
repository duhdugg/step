// worker.js

// 1. Import the current CheerpJ 3.x/4.x loader
importScripts("https://cjrtnc.leaningtech.com/4.3/loader.js");

const connectedPorts = [];
let isInitialized = false;
let isLoading = false;
let lib = null;
let bridge = null;

const memoryCache = new Map();

async function broadcast(e) {
  for (const port of connectedPorts) {
    port.postMessage(e);
  }
}

async function listHomes() {
  const response = await fetch("/rest/module/listHomes");
  const data = await response.json();
  return data;
}

function encodePath(path) {
  const reslash = new RegExp("\/", "g");
  const encodedPath = path.replace(reslash, "::");
  return encodedPath;
}

async function getFileHandleFromPath(path, create = false) {
  const segments = path.split("/");
  let currentHandle = await navigator.storage.getDirectory();
  for (let i = 0; i < segments.length - 1; i++) {
    currentHandle = await currentHandle.getDirectoryHandle(segments[i], {
      create,
    });
  }
  const fileName = segments[segments.length - 1];
  return await currentHandle.getFileHandle(fileName, { create });
}

async function getData(path) {
  const encodedPath = encodePath(path);
  const response = await fetch(`/rest/module/getHomeFile/${encodedPath}`);
  const data = await response.json();
  return data;
}

async function saveData(path, data) {
  const handle = await getFileHandleFromPath(path, true);
  const stream = await handle.createWritable();
  await stream.write(data);
  await stream.close();
}

async function getOfflineData(path) {
  const handle = await getFileHandleFromPath(path, false);
  const file = await handle.getFile();
  return file.text();
}

async function listOPFS(directoryHandle, path = "") {
  const files = [];
  // Default to the root if no handle is provided
  if (!directoryHandle) {
    directoryHandle = await navigator.storage.getDirectory();
  }
  for await (const [name, handle] of directoryHandle.entries()) {
    const relativePath = path ? `${path}/${name}` : name;

    if (handle.kind === "directory") {
      // Recurse into subdirectories
      const subFiles = await listOPFS(handle, relativePath);
      files.push(...subFiles);
    } else {
      // It's a file
      const file = await handle.getFile();
      files.push({
        path: relativePath,
        size: file.size,
        lastModified: Math.floor(file.lastModified),
      });
    }
  }
  return files;
}

async function deleteOPFSFile(path) {
  // Split and clean the path
  const segments = path.split("/").filter(Boolean);
  if (segments.length === 0) return;

  const fileName = segments.pop(); // The last part is the file
  let currentHandle = await navigator.storage.getDirectory();

  try {
    // Navigate to the parent directory
    for (const segment of segments) {
      currentHandle = await currentHandle.getDirectoryHandle(segment);
    }

    // Remove the file entry
    await currentHandle.removeEntry(fileName);
    console.log(`Deleted: ${path}`);
  } catch (e) {
    if (e.name === "NotFoundError") {
      console.warn(`File not found, nothing to delete: ${path}`);
    } else {
      throw e;
    }
  }
}

async function initializeVfsAndBridge() {
  const Bridge = await lib.org.stepbible.wasm.Bridge;
  const homeFiles = await listHomes();
  for (const homeFile of homeFiles) {
    const path = homeFile.path;
    const targetVfsPath = `/files/step/homes/${path}`;
    console.debug({ targetVfsPath: path });
    if (homeFile.mtime > (await Bridge.getFileMtime(targetVfsPath))) {
      const data = await getData(path);
      console.debug({ writing: path });
      await Bridge.writeBinaryFile(targetVfsPath, data, homeFile.lastModified);
      console.debug({ done: path });
    } else {
      console.debug({ cached: path });
    }
  }
  return await Bridge();
}

async function clearOPFS() {
  try {
    const root = await navigator.storage.getDirectory();
    await root.remove({ recursive: true });
    console.log("OPFS cleared successfully.");
  } catch (error) {
    console.error("Failed to clear OPFS:", error);
  }
}

self.onconnect = async (event) => {
  const port = event.ports[0];
  connectedPorts.push(port);
  port.start();

  if (!isInitialized && !isLoading) {
    isLoading = true;
    broadcast({ value: "worker:loading" });
    await cheerpjInit();
    const jarUrl = "/js/step-wasm.jar";
    const jarRes = await fetch(jarUrl);
    const jarBuffer = await jarRes.arrayBuffer();
    cheerpOSAddStringFile("/str/step-wasm.jar", new Uint8Array(jarBuffer));
    lib = await cheerpjRunLibrary("/str/step-wasm.jar");
    bridge = await initializeVfsAndBridge();
    isInitialized = true;
    broadcast({ value: "worker:ready" });
  } else if (isInitialized) {
    port.postMessage({ value: "worker:ready" });
  }

  const handleRequest = async (e, controllerName, methodName) => {
    const { id, args } = e.data;
    try {
      const controller = await bridge[controllerName];
      const response = await controller[methodName](...args);
      const ObjectMapper =
        await lib.com.fasterxml.jackson.databind.ObjectMapper;
      const mapper = await new ObjectMapper();
      const value = JSON.parse(await mapper.writeValueAsString(response));
      port.postMessage({ id, value });
    } catch (err) {
      console.error(err);
      if (err.printStackTrace) {
        await err.printStackTrace();
      }
      throw err;
    }
  };

  const messageRouter = {
    getQuickInfo: (e) => handleRequest(e, "moduleController", "getQuickInfo"),
    masterSearch: (e) => handleRequest(e, "searchController", "masterSearch"),
    default: (e) => {
      console.warn("unhandled action", e);
      port.postMessage({ id: e.data.id, error: "unhandled action" });
    },
  };
  port.onmessage = async function (e) {
    console.debug("worker message received", e);
    (messageRouter[e.data.action] || messageRouter.default)(e);
  };
};
