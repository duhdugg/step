// this overrides the original jquery $.get method
// with one that loads data from WASM when stepOffline is true
(function ($) {
  const originalGet = $.get;

  $.get = function (url, data, callback, type) {
    // 1. If not offline, fall back directly to original $.get
    if (!globalThis.stepOffline) {
      return originalGet.apply(this, arguments);
    }

    // 2. Normalize jQuery $.get signature options
    // $.get(url, [data], [callback], [type])
    if (typeof data === "function") {
      type = callback;
      callback = data;
      data = undefined;
    }

    // 3. Create a Deferred object to mimic jQuery's jqXHR
    const deferred = $.Deferred();

    // Attach legacy .error() method for jQuery < 3.0 backward compatibility
    deferred.promise().error = deferred.fail;

    // Attach user-supplied success callback if provided
    if (typeof callback === "function") {
      deferred.done(callback);
    }

    // 4. Dispatch request to the worker client
    let action = url
    let args = { url, data, type };
    const splitUrl = url.split('/');
    if (splitUrl.slice(0, 3).join('/') === 'rest/search/masterSearch') {
      action = 'masterSearch';
      args = splitUrl.slice(splitUrl.indexOf('masterSearch')+1).map(decodeURIComponent);
    }
    globalThis.wasmWorkerClient(action, args)
      .then((response) => {
        console.log(response, 'response');
        // Handle worker-level success vs error payloads
        if (response && response.error) {
          deferred.reject(response.error);
        } else {
          deferred.resolve(response, "success", {getResponseHeader: () => ''}, deferred);
        }
      })
      .catch((err) => {
        console.error('error', err);
        deferred.reject(err);
      });

    // 5. Return the chainable Promise object
    return deferred.promise();
  };
})(jQuery);
