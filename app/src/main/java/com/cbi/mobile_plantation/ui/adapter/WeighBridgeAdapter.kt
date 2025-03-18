package com.cbi.mobile_plantation.ui.adapter

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.weighBridge.ScanWeighBridgeActivity.InfoType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WBData(
    val id: Int,
    val dept_ppro : Int,
    val divisi_ppro :Int,
    val commodity : Int,
    val blok_jjg : String,
    val nopol : String,
    val driver : String,
    val pemuat_id : String,
    val transporter_id :Int,
    val mill_id : Int,
    val created_by_id : Int,
    val created_at : String,
    val noSPB: String,
    val estate: String,
    val afdeling: String,
    val datetime: String,
    val status_upload_cmp_wb: Int?,
    val status_upload_ppro_wb: Int?,
    val uploaded_at_wb: String?,
    val uploaded_at_ppro_wb: String?,
    val uploaded_wb_response:String?,
    val uploaded_ppro_response:String?,
    val formattedBlokList :String?,
    val pemuat_nama :String?,
    val totalJjg :Int?,
    val mill_name :String?,
    val transporter_name :String?,
)

class WeighBridgeAdapter(private var items: List<WBData>) :
    RecyclerView.Adapter<WeighBridgeAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<WBData>() // Track selected items

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val td1: TextView = view.findViewById(R.id.td1)
        val td2: TextView = view.findViewById(R.id.td2)
        val td3: TextView = view.findViewById(R.id.td3)
        val td5: LinearLayout = view.findViewById(R.id.td5) // Change to LinearLayout
        val checkbox: CheckBox = view.findViewById(R.id.checkBoxPanen) // Add this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.table_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.td1.visibility = View.VISIBLE
        holder.td2.visibility = View.VISIBLE
        holder.td3.visibility = View.VISIBLE
        holder.td5.visibility = View.VISIBLE

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val bottomSheetDialog = BottomSheetDialog(context)
            val view = LayoutInflater.from(context)
                .inflate(R.layout.layout_bottom_sheet_detail_table_wb, null)

            view.findViewById<TextView>(R.id.titleDialogDetailTable).text =
                "Detail E-SPB ${item.noSPB}"

            view.findViewById<Button>(R.id.btnCloseDetailTable).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setContentView(view)

            val maxHeight = (context.resources.displayMetrics.heightPixels * 0.85).toInt()

            bottomSheetDialog.show()

            val infoItems = listOf(
                Info.ESPB to item.noSPB,
                Info.DATE to item.datetime,
                Info.ESTATE to item.estate,
                Info.AFDELING to item.afdeling,
                Info.NOPOL to item.nopol,
                Info.BLOK to item.formattedBlokList,
                Info.TOTAL_JJG to item.totalJjg.toString(),
                Info.PEMUAT to item.pemuat_id,
                Info.DRIVER to item.driver,
                Info.MILL to item.mill_name,
                Info.TRANSPORTER to item.transporter_name,
            )
            val statusContainer = view.findViewById<LinearLayout>(R.id.statusContainer)

            setStatusCard("CMP", statusContainer, item.status_upload_cmp_wb!!, item.uploaded_at_wb ?: "", item.uploaded_wb_response ?:"")
            setStatusCard("PPRO", statusContainer, item.status_upload_ppro_wb!!, item.uploaded_at_ppro_wb ?: "", item.uploaded_ppro_response ?:"")

            infoItems.forEach { (type, value) ->
                val itemView = view.findViewById<View>(type.id)

                if (itemView != null) {
                    if (value != null) {
                        setInfoItemValues(itemView, type.label, value)
                    }
                }
            }

            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?.let { bottomSheet ->
                    val behavior = BottomSheetBehavior.from(bottomSheet)

                    behavior.apply {
                        this.peekHeight = maxHeight
                        this.state = BottomSheetBehavior.STATE_EXPANDED
                        this.isFitToContents = true
                        this.isDraggable = false
                    }

                    bottomSheet.layoutParams?.height = maxHeight
                }
        }
        holder.td1.text = item.noSPB
        holder.td2.text = "${item.estate}\n${item.afdeling}"
        holder.td3.text = item.datetime


        holder.checkbox.isChecked = selectedItems.contains(item)

        // Handle checkbox click
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(item)
            } else {
                selectedItems.remove(item)
            }
        }

        if (item.status_upload_cmp_wb == 1 && item.status_upload_ppro_wb == 1) {
            holder.checkbox.apply {
//                isChecked = true
                isEnabled = false
                alpha = 0.5f
            }
        } else {
            holder.checkbox.isEnabled = true
            holder.checkbox.alpha = 1.0f
        }


        val statusLayout = LinearLayout(holder.itemView.context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // CMP Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // CMP Text
                addView(TextView(context).apply {
                    text = "CMP"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // CMP Icon
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_upload_cmp_wb == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_upload_cmp_wb == 1) {
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    } else {
                        ContextCompat.getColor(context, R.color.colorRedDark)
                    }
                    setColorFilter(color)
                })
            })

            // PPRO Row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                // PPRO Text
                addView(TextView(context).apply {
                    text = "PPRO"
                    gravity = Gravity.START
                    typeface = ResourcesCompat.getFont(context, R.font.manrope_extrabold) // Add font family and make it bold
                    setTextColor(ResourcesCompat.getColor(resources, R.color.black, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        marginEnd = 4.dpToPx(context)
                    }
                })

                // PPRO Icon
                addView(ImageView(context).apply {
                    setImageResource(
                        if (item.status_upload_ppro_wb == 1) R.drawable.baseline_check_box_24
                        else R.drawable.baseline_close_24
                    )
                    layoutParams = LinearLayout.LayoutParams(
                        24.dpToPx(context),
                        24.dpToPx(context)
                    )
                    val color = if (item.status_upload_ppro_wb == 1) {
                        ContextCompat.getColor(context, R.color.greendarkerbutton)
                    } else {
                        ContextCompat.getColor(context, R.color.colorRedDark)
                    }
                    setColorFilter(color)
                })
            })
        }

        holder.td5.removeAllViews()
        holder.td5.addView(statusLayout)
    }

    fun setStatusCard(
        type: String,
        parentView: ViewGroup,
        status: Int,
        uploadedAt: String,
        uploaded_response:String,
    ) {
        val inflater = LayoutInflater.from(parentView.context)
        val cardView = inflater.inflate(R.layout.layout_status_upload_wb, parentView, false) as MaterialCardView

        val iconStatusAlertMessage = cardView.findViewById<ImageView>(R.id.iconStatusAlertMessage)
        val statusAlertMessage = cardView.findViewById<TextView>(R.id.statusAlertMessage)
        val uploadDate = cardView.findViewById<TextView>(R.id.upload_date)
        val titleEndpoint = cardView.findViewById<TextView>(R.id.titleEndpoint)
        val messageResponseUpload = cardView.findViewById<TextView>(R.id.messageResponseUpload)

        titleEndpoint.text = "#$type:"

        val formattedDate = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            val date = inputFormat.parse(uploadedAt)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "-"
        }

        uploadDate.text = formattedDate

        val statusText = when (status) {
            1 -> "SUCCESS"
            0 -> "FAILED"
            else -> "PENDING"
        }

        when (statusText) {
            "SUCCESS" -> {
                statusAlertMessage.text = "SUCCESS"
                statusAlertMessage.setTextColor(ContextCompat.getColor(parentView.context, R.color.greendarkerbutton))
                iconStatusAlertMessage.setImageResource(R.drawable.baseline_check_24)
            }
            "FAILED" -> {
                statusAlertMessage.text = "FAILED"
                statusAlertMessage.setTextColor(ContextCompat.getColor(parentView.context, R.color.colorRedDark))
                iconStatusAlertMessage.setImageResource(R.drawable.baseline_close_24)
                iconStatusAlertMessage.setColorFilter(
                    ContextCompat.getColor(parentView.context, R.color.colorRedDark),
                )
            }
            else -> {
                statusAlertMessage.text = "PENDING"
                statusAlertMessage.setTextColor(ContextCompat.getColor(parentView.context, R.color.graytextdark))
                iconStatusAlertMessage.setImageResource(R.drawable.baseline_file_upload_24)
                iconStatusAlertMessage.setColorFilter(
                    ContextCompat.getColor(parentView.context, R.color.graytextdark),
                )
            }
        }

        messageResponseUpload.text = when {
            type == "CMP" && uploaded_response.isNotEmpty() -> "Upload Berhasil"
            type == "PPRO" && status == 1 -> "Upload Berhasil"
            else -> uploaded_response
        }

        parentView.addView(cardView)
    }



    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun setInfoItemValues(view: View, label: String, value: String) {

        view.findViewById<TextView>(R.id.tvLabel)?.text = label

        view.findViewById<TextView>(R.id.tvValue)?.text = when (view.id) {
            R.id.infoBlok -> value
            else -> ": $value"
        }
    }

    fun updateList(newList: List<WBData>) {
        items = newList
        selectedItems.clear() // Clear selection on update
        notifyDataSetChanged()
    }

    fun getSelectedItemsIdLocal(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: "")
            )
        }
    }

    fun getSelectedItemsForUpload(): List<Map<String, Any>> {
        return selectedItems.map { selectedItem ->
            mapOf(
                "id" to (selectedItem.id ?: ""),
                "dept_ppro" to (selectedItem.dept_ppro ?: ""),
                "divisi_ppro" to (selectedItem.divisi_ppro ?: ""),
                "commodity" to (selectedItem.commodity ?: ""),
                "blok_jjg" to (selectedItem.blok_jjg ?: ""),
                "nopol" to (selectedItem.nopol ?: ""),
                "driver" to (selectedItem.driver ?: ""),
                "pemuat_id" to (selectedItem.pemuat_id ?: ""),
                "transporter_id" to (selectedItem.transporter_id ?: ""),
                "mill_id" to (selectedItem.mill_id ?: ""),
                "created_by_id" to (selectedItem.created_by_id ?: ""),
                "created_at" to (selectedItem.created_at ?: ""),
                "no_espb" to (selectedItem.noSPB ?: ""),
            )
        }
    }

    fun selectAllItems(selectAll: Boolean) {
        if (selectAll) {
            selectedItems.addAll(items.filter { it.status_upload_cmp_wb != 1 || it.status_upload_ppro_wb != 1 })
        } else {
            selectedItems.clear()
        }
        notifyDataSetChanged()
    }

    enum class Info(val id: Int, val label: String) {
        ESPB(R.id.noEspbTitleScanWB, "e-SPB"),
        DATE(R.id.infoCreatedAt, "Tanggal Buat"),
        ESTATE(R.id.infoEstate, "Estate"),
        AFDELING(R.id.infoAfdeling, "Afdeling"),
        NOPOL(R.id.infoNoPol, "No. Polisi"),
        BLOK(R.id.infoBlok, "Blok"),
        TOTAL_JJG(R.id.infoTotalJjg, "Total Janjang"),
        PEMUAT(R.id.infoPemuat, "Pemuat"),
        DRIVER(R.id.infoNoDriver, "Driver"),
        MILL(R.id.infoMill, "Mill"),
        TRANSPORTER(R.id.infoTransporter, "Transporter")
    }


    override fun getItemCount() = items.size
}