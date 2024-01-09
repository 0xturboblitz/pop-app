package expo.modules.passportreader

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.widget.EditText

import androidx.appcompat.app.AppCompatActivity
// import io.tradle.nfc.ImageUtil.decodeImage
import net.sf.scuba.smartcards.CardService
import org.apache.commons.io.IOUtils

import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.asn1.cms.SignedData
import org.bouncycastle.asn1.ASN1Primitive
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.ASN1Set
import org.bouncycastle.asn1.x509.Certificate
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.jce.interfaces.ECPublicKey


import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.SecurityInfo
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.iso19794.FaceImageInfo

import org.json.JSONObject

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.CertPathValidator
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PSSParameterSpec
import java.text.ParseException
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.*
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

import com.google.gson.Gson;

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReadableNativeMap
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Arguments
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Callback

import android.app.Activity
import expo.modules.kotlin.Promise
import expo.modules.core.interfaces.ReactActivityLifecycleListener


class PassportReaderModule : Module(), ReactActivityLifecycleListener {
  // Each module class must implement the definition function. The definition consists of components
  // that describes the module's functionality and behavior.
  // See https://docs.expo.dev/modules/module-api for more details about available components.

  private var scanPromise: Promise? = null
  private var opts: ReadableMap? = null

  data class Data(val id: String, val digest: String, val signature: String, val publicKey: String)

  data class PassportData(
    val dg1File: DG1File,
    val dg2File: DG2File,
    val sodFile: SODFile
  )

  interface DataCallback {
    fun onDataReceived(data: String)
  }
  
  private fun resetState() {
    scanPromise = null
    opts = null
  }

  override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
    // instance = this
    // appContext.reactContext.addLifecycleEventListener(this)
  }

  fun onDestroy() {
    resetState()
  }

  override fun onResume(activity: Activity) {
    val mNfcAdapter = NfcAdapter.getDefaultAdapter(activity)
    mNfcAdapter?.let {
        val intent = Intent(activity, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE) // PendingIntent.FLAG_UPDATE_CURRENT
        val filter = arrayOf(arrayOf(IsoDep::class.java.name))
        mNfcAdapter.enableForegroundDispatch(activity, pendingIntent, null, filter)
    }
  }

  override fun onPause(activity: Activity) {
      val mNfcAdapter = NfcAdapter.getDefaultAdapter(activity)
      mNfcAdapter?.disableForegroundDispatch(activity)
  }

  override fun onNewIntent(intent: Intent): Boolean {
    // super.onNewIntent(intent);
    // PassportReaderModule.Companion.getInstance().receiveIntent(intent);
    Log.d("RNPassportReaderModule", "receiveIntent: " + intent.action)
    if (scanPromise == null) return false
    if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
        val tag: Tag? = intent.extras?.getParcelable(NfcAdapter.EXTRA_TAG)
        if (tag?.techList?.contains("android.nfc.tech.IsoDep") == true) {
            val passportNumber = opts?.getString(PARAM_DOC_NUM)
            val expirationDate = opts?.getString(PARAM_DOE)
            val birthDate = opts?.getString(PARAM_DOB)
            if (!passportNumber.isNullOrEmpty() && !expirationDate.isNullOrEmpty() && !birthDate.isNullOrEmpty()) {
                val bacKey: BACKeySpec = BACKey(passportNumber, birthDate, expirationDate)
                ReadTask(IsoDep.get(tag), bacKey).execute()
            }
        }
    }
    return true;
  }

  override fun definition() = ModuleDefinition {
    // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
    // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
    // The module will be accessible from `requireNativeModule('PassportReader')` in JavaScript.
    Name("PassportReader")

    // Sets constant properties on the module. Can take a dictionary or a closure that returns a dictionary.
    Constants(
      "PI" to Math.PI
    )

    // Defines event names that the module can send to JavaScript.
    Events("onChange")

    // Defines a JavaScript synchronous function that runs the native code on the JavaScript thread.
    Function("hello") {
      "Hello world! 👋"
    }

    AsyncFunction("scan") { options: ReadableMap, promise: Promise ->
      val mNfcAdapter = NfcAdapter.getDefaultAdapter(appContext.reactContext)
      if (mNfcAdapter == null) {
          promise.reject("E_NOT_SUPPORTED", "NFC chip reading not supported", null)
          return@AsyncFunction
      }

      if (!mNfcAdapter.isEnabled) {
          promise.reject("E_NOT_ENABLED", "NFC chip reading not enabled", null)
          return@AsyncFunction
      }

      if (scanPromise != null) {
          promise.reject("E_ONE_REQ_AT_A_TIME", "Already running a scan", null)
          return@AsyncFunction
      }

      opts = options
      scanPromise = promise
      Log.d("RNPassportReaderModule", "opts set to: " + opts.toString())
    }
  }

  private inner class ReadTask(private val isoDep: IsoDep, private val bacKey: BACKeySpec) : AsyncTask<Void?, Void?, Exception?>() {

    private lateinit var dg1File: DG1File
    private lateinit var dg2File: DG2File
    private lateinit var dg14File: DG14File
    private lateinit var sodFile: SODFile
    private var imageBase64: String? = null
    private var bitmap: Bitmap? = null
    private var chipAuthSucceeded = false
    private var passiveAuthSuccess = false
    private lateinit var dg14Encoded: ByteArray

    override fun doInBackground(vararg params: Void?): Exception? {
        try {
            isoDep.timeout = 10000
            Log.e("MY_LOGS", "This should obvsly log")
            val cardService = CardService.getInstance(isoDep)
            Log.e("MY_LOGS", "cardService gotten")
            cardService.open()
            Log.e("MY_LOGS", "cardService opened")
            val service = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                false,
            )
            Log.e("MY_LOGS", "service gotten")
            service.open()
            Log.e("MY_LOGS", "service opened")
            var paceSucceeded = false
            try {
                Log.e("MY_LOGS", "trying to get cardAccessFile...")
                val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                Log.e("MY_LOGS", "cardAccessFile: ${cardAccessFile}")

                val securityInfoCollection = cardAccessFile.securityInfos
                for (securityInfo: SecurityInfo in securityInfoCollection) {
                    if (securityInfo is PACEInfo) {
                        Log.e("MY_LOGS", "trying PACE...")
                        service.doPACE(
                            bacKey,
                            securityInfo.objectIdentifier,
                            PACEInfo.toParameterSpec(securityInfo.parameterId),
                            null,
                        )
                        Log.e("MY_LOGS", "PACE succeeded")
                        paceSucceeded = true
                    }
                }
            } catch (e: Exception) {
                Log.w("MY_LOGS", e)
            }
            Log.e("MY_LOGS", "Sending select applet command with paceSucceeded: ${paceSucceeded}") // this is false so PACE doesn't succeed
            service.sendSelectApplet(paceSucceeded)
            if (!paceSucceeded) {
                try {
                    Log.e("MY_LOGS", "trying to get EF_COM...")
                    service.getInputStream(PassportService.EF_COM).read()
                } catch (e: Exception) {
                    Log.e("MY_LOGS", "doing BAC")
                    service.doBAC(bacKey) // <======================== error happens here
                    Log.e("MY_LOGS", "BAC done")
                }
            }

            val dg1In = service.getInputStream(PassportService.EF_DG1)
            dg1File = DG1File(dg1In)   
            val dg2In = service.getInputStream(PassportService.EF_DG2)
            dg2File = DG2File(dg2In)                
            val sodIn = service.getInputStream(PassportService.EF_SOD)
            sodFile = SODFile(sodIn)

            doChipAuth(service)
            doPassiveAuth()

            val allFaceImageInfo: MutableList<FaceImageInfo> = ArrayList()
            dg2File.faceInfos.forEach {
                allFaceImageInfo.addAll(it.faceImageInfos)
            }
            if (allFaceImageInfo.isNotEmpty()) {
                val faceImageInfo = allFaceImageInfo.first()
                val imageLength = faceImageInfo.imageLength
                val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                val buffer = ByteArray(imageLength)
                dataInputStream.readFully(buffer, 0, imageLength)
                val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
                // bitmap = decodeImage(appContext.reactContext, faceImageInfo.mimeType, inputStream)
                imageBase64 = Base64.encodeToString(buffer, Base64.DEFAULT)
            }
        } catch (e: Exception) {
            return e
        }
        return null
    }

    private fun doChipAuth(service: PassportService) {
        try {
            val dg14In = service.getInputStream(PassportService.EF_DG14)
            dg14Encoded = IOUtils.toByteArray(dg14In)
            val dg14InByte = ByteArrayInputStream(dg14Encoded)
            dg14File = DG14File(dg14InByte)
            val dg14FileSecurityInfo = dg14File.securityInfos
            for (securityInfo: SecurityInfo in dg14FileSecurityInfo) {
                if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                    service.doEACCA(
                        securityInfo.keyId,
                        ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                        securityInfo.objectIdentifier,
                        securityInfo.subjectPublicKey,
                    )
                    chipAuthSucceeded = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
    }

    private fun doPassiveAuth() {
        try {
            Log.d(TAG, "Starting passive authentication...")
            val digest = MessageDigest.getInstance(sodFile.digestAlgorithm)
            Log.d(TAG, "Using digest algorithm: ${sodFile.digestAlgorithm}")

            
            val dataHashes = sodFile.dataGroupHashes
            
            val dg14Hash = if (chipAuthSucceeded) digest.digest(dg14Encoded) else ByteArray(0)
            val dg1Hash = digest.digest(dg1File.encoded)
            val dg2Hash = digest.digest(dg2File.encoded)
            
            Log.d(TAG, "Comparing data group hashes...")

            if (Arrays.equals(dg1Hash, dataHashes[1]) && Arrays.equals(dg2Hash, dataHashes[2])
                && (!chipAuthSucceeded || Arrays.equals(dg14Hash, dataHashes[14]))) {

                Log.d(TAG, "Data group hashes match.")

                val asn1InputStream = ASN1InputStream(appContext?.reactContext?.assets?.open("masterList"))
                val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
                keystore.load(null, null)
                val cf = CertificateFactory.getInstance("X.509")

                var p: ASN1Primitive?
                var obj = asn1InputStream.readObject()

                while (obj != null) {
                    p = obj
                    val asn1 = ASN1Sequence.getInstance(p)
                    if (asn1 == null || asn1.size() == 0) {
                        throw IllegalArgumentException("Null or empty sequence passed.")
                    }

                    if (asn1.size() != 2) {
                        throw IllegalArgumentException("Incorrect sequence size: " + asn1.size())
                    }
                    val certSet = ASN1Set.getInstance(asn1.getObjectAt(1))
                    for (i in 0 until certSet.size()) {
                        val certificate = Certificate.getInstance(certSet.getObjectAt(i))
                        val pemCertificate = certificate.encoded
                        val javaCertificate = cf.generateCertificate(ByteArrayInputStream(pemCertificate))
                        keystore.setCertificateEntry(i.toString(), javaCertificate)
                    }
                    obj = asn1InputStream.readObject()

                }

                val docSigningCertificates = sodFile.docSigningCertificates
                Log.d(TAG, "Checking document signing certificates for validity...")
                for (docSigningCertificate: X509Certificate in docSigningCertificates) {
                    docSigningCertificate.checkValidity()
                    Log.d(TAG, "Certificate: ${docSigningCertificate.subjectDN} is valid.")
                }

                val cp = cf.generateCertPath(docSigningCertificates)
                val pkixParameters = PKIXParameters(keystore)
                pkixParameters.isRevocationEnabled = false
                val cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType())
                Log.d(TAG, "Validating certificate path...")
                cpv.validate(cp, pkixParameters)
                var sodDigestEncryptionAlgorithm = sodFile.docSigningCertificate.sigAlgName
                var isSSA = false
                if ((sodDigestEncryptionAlgorithm == "SSAwithRSA/PSS")) {
                    sodDigestEncryptionAlgorithm = "SHA256withRSA/PSS"
                    isSSA = true

                }
                val sign = Signature.getInstance(sodDigestEncryptionAlgorithm)
                if (isSSA) {
                    sign.setParameter(PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1))
                }
                sign.initVerify(sodFile.docSigningCertificate)
                sign.update(sodFile.eContent)

                passiveAuthSuccess = sign.verify(sodFile.encryptedDigest)
                Log.d(TAG, "Passive authentication success: $passiveAuthSuccess")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception in passive authentication", e)
        }
    }


    override fun onPostExecute(result: Exception?) {
      if (scanPromise == null) return

      if (result != null) {
          if (result is IOException) {
              scanPromise?.reject("E_SCAN_FAILED_DISCONNECT", "Lost connection to chip on card", null)
          }

          resetState()
          return
      }

      val mrzInfo = dg1File.mrzInfo
      val gson = Gson()
      val signedDataField = SODFile::class.java.getDeclaredField("signedData")
      signedDataField.isAccessible = true
      
      val eContentAsn1InputStream = ASN1InputStream(sodFile.eContent.inputStream())

      val passport = Arguments.createMap()
      passport.putString("mrz", mrzInfo.toString())
      passport.putString("signatureAlgorithm", sodFile.docSigningCertificate.sigAlgName) // this one is new

      val publicKey = sodFile.docSigningCertificate.publicKey
      if (publicKey is RSAPublicKey) {
          passport.putString("modulus", publicKey.modulus.toString())
      } else if (publicKey is ECPublicKey) {
          passport.putString("publicKeyQ", publicKey.q.toString())
      }

      passport.putString("dataGroupHashes", gson.toJson(sodFile.dataGroupHashes))
      passport.putString("eContent", gson.toJson(sodFile.eContent))
      passport.putString("encryptedDigest", gson.toJson(sodFile.encryptedDigest))

      scanPromise?.resolve(passport)
      resetState()
    }
  }

  private fun convertDate(input: String?): String? {
    if (input == null) {
        return null
    }
    return try {
        SimpleDateFormat("yyMMdd", Locale.US).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(input)!!)
    } catch (e: ParseException) {
        // Log.w(RNPassportReaderModule::class.java.simpleName, e)
        null
    }
  }

  companion object {
    private val TAG = PassportReaderModule::class.java.simpleName
    private const val PARAM_DOC_NUM = "documentNumber";
    private const val PARAM_DOB = "dateOfBirth";
    private const val PARAM_DOE = "dateOfExpiry";
    const val JPEG_DATA_URI_PREFIX = "data:image/jpeg;base64,"
    var instance: PassportReaderModule? = null
  }
}
