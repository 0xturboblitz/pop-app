import ExpoModulesCore
import QKMRZScanner

// This view will be used as a native component. Make sure to inherit from `ExpoView`
// to apply the proper styling (e.g. border radius and shadows).

class Ocr_moduleView: ExpoView {
  var mrzScannerView: QKMRZScannerView!

  required init(appContext: AppContext? = nil) {
    super.init(appContext: appContext)
    clipsToBounds = true
    addSubview(mrzScannerView)
  }

  func startScanning() {
    mrzScannerView = QKMRZScannerView(frame: self.bounds)
    mrzScannerView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    addSubview(mrzScannerView)
    mrzScannerView.startScanning()
  }

  func stopScanning() {
    mrzScannerView.stopScanning()
  }
  
}