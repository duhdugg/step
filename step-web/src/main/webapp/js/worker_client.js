export function createWorkerClient(port, onReady) {
  const self = { onReady, pending: new Map() };

  port.onmessage = (e) => {
    const { id, value, error } = e.data;
    if (value === 'worker:ready') {
      self.onReady();
      return;
    }
    if (self.pending.has(id)) {
      const {resolve, reject} = self.pending.get(id);
      if (error) {
        reject(error);
      } else {
        resolve(value);
      }
      self.pending.delete(id);
    }
  };

  return (action, args) =>
    new Promise((resolve, reject) => {
      const id = crypto.randomUUID();
      self.pending.set(id, {resolve, reject});
      port.postMessage({ id, action, args });
    });
}
