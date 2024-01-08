import ExpoModulesCore
import QKMRZScanner

public class Ocr_moduleModule: Module {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.
  public func definition() -> ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('Ocr_module')` in JavaScript.
    Name("Ocr_module")

    // Sets constant properties on the module. Can take a dictionary or a closure that returns a dictionary.
    Constants([
      "PI": Double.pi
    ])

    // Defines event names that the module can send to JavaScript.
    Events("onChange")

    // Defines a JavaScript synchronous function that runs the native code on the JavaScript thread.
    Function("hello") {
      return "Hello world! 👋"
    }

    // Defines a JavaScript function that always returns a Promise and whose native code
    // is by default dispatched on the different thread than the JavaScript runtime runs on.
    AsyncFunction("setValueAsync") { (value: String) in
      // Send an event to JavaScript.
      self.sendEvent("onChange", [
        "value": value
      ])
    }

    // Enables the module to be used as a native view. Definition components that are accepted as part of the
    // view definition: Prop, Events.
    View(Ocr_moduleView.self) {
      // Defines a setter for the `name` prop.
      Prop("name") { (view: Ocr_moduleView, prop: String) in
        print(prop)
      }
    }

    AsyncFunction("startScanning") { (promise: Promise) in
      DispatchQueue.main.async {
        if let view = self.appContext?.viewRegistry?.view(forId: self.viewId, ofClass: Ocr_moduleView.self) as? Ocr_moduleView {
          view.startScanning()
          promise.resolve("Scanning started")
        } else {
          promise.reject("E_VIEW_NOT_FOUND", "Ocr_moduleView not found", nil)
        }
      }
    }

    AsyncFunction("stopScanning") { (promise: Promise) in
      DispatchQueue.main.async {
        if let view = self.appContext?.viewRegistry?.view(forId: self.viewId, ofClass: Ocr_moduleView.self) as? Ocr_moduleView {
          view.stopScanning()
          promise.resolve(nil) // Resolve the promise if scanning is successfully stopped
        } else {
          promise.reject("E_VIEW_NOT_FOUND", "Ocr_moduleView not found", nil) // Reject the promise if the view is not found
        }
      }
    }

  }
  // MRZScannerViewDelegate method
  func mrzScannerView(_ mrzScannerView: QKMRZScannerView, didFind scanResult: QKMRZScanResult) {
    sendEvent("onMRZScanned", [
      "scanResult": scanResult // Convert scanResult to a suitable format
    ])
  }
}
