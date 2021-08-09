package co.dapi.wusample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import co.dapi.connect.core.base.Dapi
import co.dapi.connect.core.callbacks.OnDapiTransferListener
import co.dapi.connect.data.endpoint_models.DapiAccountsResponse
import co.dapi.connect.data.models.DapiBeneficiary
import co.dapi.connect.data.models.DapiConnection
import co.dapi.connect.data.models.DapiError
import co.dapi.connect.data.models.LinesAddress
import co.dapi.wusample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedConnection: DapiConnection? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setTransferListener()
        setConnectButtonClickListener()
        setTransferButtonClickListener()
        setConnectionsSpinnerItemSelectedListener()
        setRecipientAccountNumber()
    }

    private fun setRecipientAccountNumber() {
        binding.tvRecipientAccountNoValue.text = getSandboxBeneficiary().accountNumber
    }

    override fun onResume() {
        super.onResume()
        initConnectionsSpinner()
    }

    private fun setConnectionsSpinnerItemSelectedListener() {
        binding.spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val name = parent?.getItemAtPosition(position) as String
                    selectConnection(name)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}

            }
    }

    private fun selectConnection(name: String) {
        Dapi.getConnections({
            selectedConnection =
                it.filter { it.name == name }.toMutableList().firstOrNull()
        }, {
            Log.e("DapiError", it.message!!)
        })
    }


    private fun setConnectButtonClickListener() {
        binding.btnConnect.setOnClickListener {
            openBanksActivity()
        }
    }

    private fun setTransferButtonClickListener() {
        binding.btnTransfer.setOnClickListener {
            if (selectedConnection != null) {
                createTransfer(selectedConnection!!, 100.0, getSandboxBeneficiary())
            }
        }
    }

    private fun openBanksActivity() {
        Intent(this, BanksActivity::class.java).apply {
            startActivity(this)
        }
    }

    private fun initConnectionsSpinner() {
        Dapi.getConnections({
            if (it.isEmpty()) {
                binding.btnTransfer.disable()
            } else {
                binding.btnTransfer.enable()
                val connectionsNames = mutableListOf<String>()
                it.onEach {
                    connectionsNames.add(it.name)
                }
                val dataAdapter =
                    ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        connectionsNames
                    )
                dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinner.adapter = dataAdapter
            }
        }, {
            Log.e("DapiError", it.message!!)
        })
    }

    private fun createTransfer(
        connection: DapiConnection,
        amount: Double,
        beneficiary: DapiBeneficiary
    ) {
        if (Dapi.isStarted) {
            connection.createTransfer(
                amount = amount,
                toBeneficiary = beneficiary
            )
        } else {
            Log.e("DapiError", "Must start the SDK before doing any operation")
        }
    }

    private fun getSandboxBeneficiary(): DapiBeneficiary {
        val lineAddress = LinesAddress()
        lineAddress.line1 = "baniyas road"
        lineAddress.line2 = "dubai"
        lineAddress.line3 = "united arab emirates"

        return DapiBeneficiary(
            address = lineAddress,
            accountNumber = "2568749867",
            name = "John Doe",
            bankName = "STANDARD CHARTERED BANK",
            swiftCode = "SCBLAEAD",
            iban = "AE0000000000002568749867",
            country = "AE",
            branchAddress = "Dubai Mall",
            branchName = "Dubai Mall",
            phoneNumber = "0581234567",
            nickname = "nickname"
        )
    }

    private fun setTransferListener() {
        Dapi.transferListener = object : OnDapiTransferListener {
            override fun onTransferSuccess(
                account: DapiAccountsResponse.DapiAccount,
                amount: Double,
                reference: String?
            ) {
                Log.i(
                    "Dapi",
                    "Transfer succeeded: amount: $amount\n" +
                            " fromAccount: $account\n" +
                            " reference: $reference"
                )
            }

            override fun onTransferFailure(
                account: DapiAccountsResponse.DapiAccount?,
                error: DapiError
            ) {
                if (error.type == DapiError.BENEFICIARY_COOL_DOWN_PERIOD) {
                    scheduleCooldownPeriodNotification()
                }

                Log.e(
                    "DapiError",
                    "Transfer failed from $account\n" +
                            " with error $error"
                )
            }

            override fun onUiDismissed() {
                Log.i("Dapi", "Transfer UI is dismissed")
            }

            override fun willTransferAmount(
                amount: Double,
                senderAccount: DapiAccountsResponse.DapiAccount
            ) {
                Log.i(
                    "Dapi",
                    "UI will transfer amount $amount to $senderAccount"
                )
            }

        }
    }

    private fun scheduleCooldownPeriodNotification() {
        selectedConnection!!.getAccountsMetaData({
            val coolDownPeriod = it.accountsMetadata.beneficiaryCoolDownPeriod
            val unit = coolDownPeriod.unit
            val value = coolDownPeriod.value
            Log.e("DapiError", "Beneficiary creation will take $value $unit")
        }, {
            Log.e("DapiError", it.message!!)
        })
    }

    private fun Button.disable() {
        isEnabled = false
        isClickable = false
        isLongClickable = false
    }

    private fun Button.enable() {
        isEnabled = true
        isClickable = true
        isLongClickable = true
    }


}