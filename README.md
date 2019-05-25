# notes-app-frontend
Notes app frontend for practicing Clojure and AWS with https://serverless-stack.com/ tutorial porting everything to Clojure(script) on the fly.

### Development mode

TODO: Write instructions for shadow-cljs

To start the Figwheel compiler, navigate to the project folder and run the following command in the terminal:

```
lein figwheel
```

Figwheel will automatically push cljs changes to the browser.
Once Figwheel starts up, you should be able to open the `public/index.html` page in the browser.


### Building for production

```
lein clean
lein package
```
