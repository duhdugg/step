# step-wasm POC

> Doug - 4/4/26

I went down the path of trying to get some of the step-core code
compiled to WASM using GraalVM for an offline browser app proof-of-concept.
The idea was to have a PWA type of app with offline capabilities that can be
installed simply by tapping "Add to Home Screen" from the browser.
I think now is a good moment to pause and write down some notes on my
findings.

Keep in mind, I'm coming at this with little to no Java experience, and the
help of Google Gemini's free tier, so if some of this seems like
I misunderstand some of what I'm talking about, that is why :)

Specifically, I focused on trying to recreate all the backend code that the
server calls at `/rest/bible/getBibleText/ESV_th/Gen1.1`.
GraalVM uses its own Java runtime and compiler, and the WASM environment
brings limitations that need worked around.

Guice dependency injection, for example, I wasn't able to get working, and the
compiler tree-shakes what it perceives to be "dead code" aggressively. To get
around this, I added declared a lot of imports in `reflect-config.json` which
told the compiler to keep those classes in the final build. But I also wasn't
able to get the server-side code to compile in this environment, so I was
working on instantiating everything needed for
`BibleInformationServiceImpl().getPassageText()`. At some point during this
process, I ended up needing to create a new class that implements `Injector`
just so the `MorphologyProcessor` could be loaded by this service. There was
some hard-coding involved that would normally be detected by guice, so other
things which need loaded by `EntityManager` instances would need to be
hard-coded in this way.

Resource-loading is fairly straightforward. I used a `resource-config.json`
file to tell the compiler to include file extensions like `.properties`, `.plugin`, etc,
and there's also a section in that file for named bundles needed like
"iso639" and "BibleNames".

Some classes I needed to override because of things WASM doesn't support.
Otherwise it would not compile, or I would hit an uncaught exception in
runtime:

- `org.crosswire.common.util.CallContext`
  * removing multi-thread support
- `org.crosswire.common.util.IOUtil`
  * removing support to unpack zip files
- `org.crosswire.common.util.NetUtil`
  * removing support for functions that
  require a network stack
- `org.crosswire.jsword.index.lucene.LuceneIndex`
  * I haven't even begun going down the path of what would be needed to allow
    searching lucene indexes, as that hasn't been my focus. But this is
    heavily reliant on background jobs and Thread creation, and I had to strip
    this down a lot.
- `com.tyndalehouse.step.core.data.entities.impl.EntityIndexReaderImpl`
  * I had to comment out the IndexSearcher from being assigned when this is
    initialized. It's another lucene-releated class.
- `org.crosswire.jsword.book.sword` and `org.crosswire.jsword.book.sword.state`
  * for example: `ZVerseBackend` and `ZVerseBackendState`
  * several classes in these packages use `java.io.RandomAccessFile`, which is
    completely incompatible with the WASM runtime, so I had to modify these to
    use a replacement class which implements all of the same methods as
    RandomAccessFile
- note: I also have some other classes copied in this repo, which I'm using just for
  debugging purposes. They have no modifications other than a bunch of
  `System.out.println` calls.


For step-web server side code, the only modification I made for my POC
implementation was adding these 2 rest endpoints:

1. `/rest/module/listHomes`
  * this lists all of the files in the `/opt/step/homes` directory
2. `/rest/module/getHomeFile/<path>`
  * this fetches the contents of the homes directory file, encoded as base64
  * examples:
    `/rest/module/getHomeFile/sword::mods.d::ESV_th.conf`
    `/rest/module/getHomeFile/sword::modules::texts::ztext::ESV_th::nt.bzs`
    `/rest/module/getHomeFile/sword::modules::texts::ztext::ESV_th::nt.bzv`

So after all that, here's the current state:

- [html POC](https://github.com/duhdugg/step/blob/graalvm-poc/step-web/src/main/webapp/html/_poc_files.html)
- [wasm POC](https://github.com/duhdugg/step/blob/graalvm-poc/step-wasm/src/main/java/org/stepbible/wasm/Bridge.java)

----

- The compiler generates a WASM file with some javascript, and I have an HTML page...
  * `step-web/src/main/webapp/html/step-engine.js.wasm`
  * `step-web/src/main/webapp/html/step-engine.js`
  * `step-web/src/main/webapp/html/_poc_files.html`
- the HTML POC loads this WASM file
- the javascript calls the `listHomes` endpoint, then iterates over each of the
  files returned and fetches the data from the `getHomeFile` endpoint, saving
  the data into the browser's [Origin private file system](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API/Origin_private_file_system)
- after all files are loaded into the OPFS and the WASM is instantiated,
  everything from the OPFS is then seeded into the WASM Virtual File System
  (VFS) so that these look like native files to the java runtime running in
  WASM
- once all of that is done, the `initEngine` method of my WASM file is called,
  which initializes the jsword config, registers the `SwordBookDriver`, and
  initializes everything needed for `BibleInformationServiceImpl`.
- after initialization is complete, I call the `getPassageText` method from
  javascript, which calls the function with this same name in WASM on the
  `BibleInformationServiceImpl` instance.


What currently works:

- the book driver is initialized and is able to read from files which were
  seeded to the VFS as if they were native files. It's able to see that I've
  seeded the files needed for one book (`ESV_th`), read the metadata for
  that book, and know where to look for passage text.

Where I'm currently blocked:

`java.lang.UnsupportedOperationException: Inflater.init`

- ZLib compression isn't going to work in WASM because of the dependency on
  `java.util.zip.Inflater`. I'm not sure about the other compression types,
  but I'm not optimistic. So we would either need to rewrite some of the
  jsword decompression-related code, or we would need to re-package the sword
  modules as `CompressType=RAW` just for this purpose. Decompressing these
  files at the `getHomeFile` endpoint by itself wouldn't be sufficient,
  even if we also switched `CompressType=ZIP` to `CompressType=RAW` in
  `ESV_th.conf` because the index files (`.bzs`, `.bzf`) are hard-coded to
  specific byte offsets.

So, in summary. I think this is maybe possible and still less effort than
developing an iOS version and/or an Android version, but there is no
shortage of challenges to overcome.
