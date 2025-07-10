package com.cbi.mobile_plantation.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel.PageData
import com.cbi.mobile_plantation.utils.AppLogger
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.card.MaterialCardView

class FormAncakFragment : Fragment() {
    data class InputMapping(
        val layoudId: Int,
        val label: String,
        val inputType: InputType,
        val dataField: (PageData, Int) -> PageData,
        val currentValue: (PageData) -> Int
    )

    private lateinit var viewModel: FormAncakViewModel
    private val errorViewsMap = mutableMapOf<Int, TextView>()
    private var isFragmentInitializing = false // Add this flag
    private var isUpdatingData = false
    private val listRadioItems: Map<String, Map<String, String>> = mapOf(
        "YesOrNoOrTitikKosong" to mapOf(
            "1" to "Ya",
            "2" to "Tidak",
            "3" to "Titik Kosong"
        ),
        "YesOrNo" to mapOf(
            "1" to "Ya",
            "2" to "Tidak"
        ),
        "HighOrLow" to mapOf(
            "1" to "Tinggi",
            "2" to "Rendah"
        ),
        "ExistsOrNot" to mapOf(
            "1" to "Ada",
            "2" to "Tidak"
        ),
        "NeatOrNot" to mapOf(
            "1" to "Standar",
            "2" to "Tidak Standar"
        ),
        "PelepahType" to mapOf(
            "1" to "Ada",
            "2" to "Tidak ada"
        ),
        "PruningType" to mapOf(
            "1" to "Standard",
            "2" to "Overpruning",
            "3" to "Underpruning"
        )
    )

    private var pageNumber: Int = 1

    companion object {
        private const val ARG_PAGE_NUMBER = "page_number"

        fun newInstance(pageNumber: Int): FormAncakFragment {
            return FormAncakFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_NUMBER, pageNumber)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageNumber = it.getInt(ARG_PAGE_NUMBER, 1)
        }

        viewModel = ViewModelProvider(requireActivity())[FormAncakViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(R.layout.fragment_form_ancak, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🚀 Set initialization flag FIRST
        isFragmentInitializing = true

        // Setup page number
        val tvNoPokok = view.findViewById<TextView>(R.id.tvNoPokokInspect)
        tvNoPokok.text = pageNumber.toString()

        // Setup title observers
        setupTitleObservers(view)

        setupAllInputs()

        // ⏰ Enable TextWatchers AFTER all setup is complete
        view.post {
            view.postDelayed({
                isFragmentInitializing = false
            }, 300)
        }
    }

    private fun setupTitleObservers(view: View) {
        val tvTitleEst = view.findViewById<TextView>(R.id.tvTitleEstFormInspect)
        viewModel.estName.observe(viewLifecycleOwner) { est ->
            tvTitleEst.text = est ?: "-"
        }

        val tvTitleAfd = view.findViewById<TextView>(R.id.tvTitleAfdFormInspect)
        viewModel.afdName.observe(viewLifecycleOwner) { afd ->
            tvTitleAfd.text = afd ?: "-"
        }

        val tvTitleBlok = view.findViewById<TextView>(R.id.tvTitleBlokFormInspect)
        viewModel.blokName.observe(viewLifecycleOwner) { blok ->
            tvTitleBlok.text = blok ?: "-"
        }
    }

    private fun setupAllInputs() {
        val itemListMapping = mapOf(
            R.id.lyExistsTreeInspect to "YesOrNoOrTitikKosong",
            R.id.lyNeatPelepahInspect to "NeatOrNot",
            R.id.lyPelepahSengklehInspect to "PelepahType",
            R.id.lyPruningInspect to "PruningType",
        )

        val inputMappings: List<InputMapping> = listOf(
            InputMapping(
                R.id.lyExistsTreeInspect,
                "Terdapat Temuan?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(emptyTree = value) },
                { it.emptyTree }
            ),

            InputMapping(
                R.id.lyHarvestTreeInspect,
                "Pokok Dipanen?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(harvestTree = value) },
                { it.harvestTree }
            ),
            InputMapping(
                R.id.lyNeatPelepahInspect,
                "Susunan Pelepah?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(neatPelepah = value) },
                { it.neatPelepah }
            ),
            InputMapping(
                R.id.lyPelepahSengklehInspect,
                "Pelepah Sengkleh?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(pelepahSengkleh = value) },
                { it.pelepahSengkleh }
            ),
            InputMapping(
                R.id.lyPruningInspect,
                "Kondisi Pruning?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(pruning = value) },
                { it.pruning }
            ),
            InputMapping(
                R.id.lyBMtidakdipotong,
                "Buah masak tidak dipotong",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(buahMasakTdkDipotong = value) },
                { it.buahMasakTdkDipotong }
            ),
            InputMapping(
                R.id.lyBTPiringanGwangan,
                "Buah tertinggal di piringan dan buah diperam digawangan mati",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(btPiringanGawangan = value) },
                { it.btPiringanGawangan }
            ),
            InputMapping(
                R.id.lyBrdKtpGawangan,
                "Brondolan dibuang ke gawangan",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdKtpGawangan = value) },
                { it.brdKtpGawangan }
            ),
            InputMapping(
                R.id.lyBrdKtpPiringan,
                "Brondolan tidak dikutip bersih di piringan, psr pikul dan ketiak pokok",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdKtpPiringanPikulKetiak = value) },
                { it.brdKtpPiringanPikulKetiak }
            ),
        )

        val currentPageData =
            viewModel.getPageData(pageNumber) ?: PageData(pokokNumber = pageNumber)

        inputMappings.forEach { (layoutId, label, inputType, dataField, currentValue) ->
            val valueForThisPage = currentValue(currentPageData)

            when (inputType) {
                InputType.RADIO -> setupRadioGroup(
                    layoutId = layoutId,
                    titleText = label,
                    itemList = listRadioItems[itemListMapping[layoutId] ?: "YesOrNo"] ?: emptyMap(),
                    dataField = dataField,
                    currentValue = valueForThisPage
                )

                InputType.EDITTEXT -> setupNumericInput(
                    layoutId = layoutId,
                    titleText = label,
                    dataField = dataField,
                    currentValue = valueForThisPage
                )

                else -> {}
            }
        }
    }


    fun scrollToTop() {
        view?.findViewById<ScrollView>(R.id.svMainFormAncakInspect)?.smoothScrollTo(0, 0)
    }

    fun showValidationError(layoutId: Int, errorMessage: String) {
        val layoutView = view?.findViewById<View>(layoutId) ?: return
        val errorTextView = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        errorTextView?.text = errorMessage

        val params = errorTextView?.layoutParams as? ViewGroup.MarginLayoutParams
        params?.topMargin = -10
        errorTextView?.visibility = View.VISIBLE

        errorViewsMap[layoutId] = errorTextView
    }

    @SuppressLint("SetTextI18n")
    fun clearValidationErrors() {
        errorViewsMap.values.forEach { errorTextView ->
            errorTextView.text = ""
            errorTextView.visibility = View.GONE
        }
        errorViewsMap.clear()
    }

    private fun savePageData() {
        val currentData = viewModel.getPageData(pageNumber) ?: PageData()
        val updatedData = currentData.copy(
            emptyTree = getRadioSelection(R.id.lyExistsTreeInspect),
        )
        viewModel.savePageData(pageNumber, updatedData)
    }

    private fun getRadioSelection(layoutId: Int): Int {
        val layoutView = view?.findViewById<View>(layoutId) ?: return 0
        val fblRadioComponents = layoutView.findViewById<FlexboxLayout>(R.id.fblRadioComponents)

        for (i in 0 until fblRadioComponents.childCount) {
            val child = fblRadioComponents.getChildAt(i)
            if (child is RadioButton && child.isChecked) {
                return child.tag as? Int ?: 0
            }
        }

        return 0
    }

    private fun setupRadioGroup(
        layoutId: Int,
        titleText: String,
        itemList: Map<String, String>,
        dataField: (PageData, Int) -> PageData,
        currentValue: Int
    ) {
        val layoutView = view?.findViewById<View>(layoutId) ?: return
        val titleTextView = layoutView.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
        val mcvSpinner = layoutView.findViewById<MaterialCardView>(R.id.MCVSpinner)
        val fblRadioComponents = layoutView.findViewById<FlexboxLayout>(R.id.fblRadioComponents)

        titleTextView.text = titleText

        if (layoutId == R.id.lyExistsTreeInspect) {
            viewModel.isInspection.observe(viewLifecycleOwner) { isInspection ->
                titleTextView.text = if (isInspection) "Terdapat Temuan?" else "Pokok Dipanen?"

                val currentData = viewModel.getPageData(pageNumber)

                if (currentData != null) {
                    updateDependentLayoutVisibility(currentData.emptyTree)
                }
            }
        }

        mcvSpinner.visibility = View.GONE
        fblRadioComponents.visibility = View.VISIBLE

        fblRadioComponents.removeAllViews()

        var lastSelectedRadioButton: RadioButton? = null

        val pageData = viewModel.getPageData(pageNumber) ?: return
        val fieldValue = currentValue

        itemList.forEach { (id, label) ->
            val idValue = id.toInt()
            val radioButton = RadioButton(layoutView.context).apply {
                text = label
                tag = idValue
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(10, 0, 30, 0)
                buttonTintList =
                    ContextCompat.getColorStateList(layoutView.context, R.color.greenDefault)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // Restore saved state
                isChecked = idValue == fieldValue
                if (isChecked) {
                    lastSelectedRadioButton = this

                }

                setOnClickListener {
                    clearValidationErrors()

                    lastSelectedRadioButton?.isChecked = false
                    isChecked = true
                    lastSelectedRadioButton = this

                    val currentData =
                        viewModel.getPageData(pageNumber) ?: PageData(pokokNumber = pageNumber)
                    val updatedData = dataField(currentData, idValue)
                    viewModel.savePageData(pageNumber, updatedData)


                    if (layoutId == R.id.lyExistsTreeInspect) {
                        updateDependentLayoutVisibility(idValue)
                    }
                }
            }

            fblRadioComponents.addView(radioButton)
        }

        // Trigger dependent layout visibility for the first field
        if (layoutId == R.id.lyExistsTreeInspect) {
            updateDependentLayoutVisibility(fieldValue)
        }
    }

    fun updatePageData() {
        isUpdatingData = true  // 🚨 Disable TextWatchers

        setupAllInputs()  // Re-setup all inputs with fresh data

        // Re-enable TextWatchers after a short delay
        view?.post {
            isUpdatingData = false
        }
    }

    private fun setupNumericInput(
        layoutId: Int,
        titleText: String,
        dataField: (PageData, Int) -> PageData,
        currentValue: Int,
        minValue: Int = 0,
        maxValue: Int? = null
    ) {
        val layoutView = view?.findViewById<View>(layoutId) ?: return
        val titleTextView = layoutView.findViewById<TextView>(R.id.tvNumberPanen)
        val editText = layoutView.findViewById<EditText>(R.id.etNumber)
        val btnMinus = layoutView.findViewById<CardView>(R.id.btDec)
        val btnPlus = layoutView.findViewById<CardView>(R.id.btInc)

        titleTextView.text = titleText
        editText.setText(currentValue.toString())  // ✅ KEEP THIS

        // Plus button setup
        handleLongPress(editText, btnPlus, dataField, minValue, maxValue)
        btnPlus.setOnClickListener {
            val currentVal = editText.text.toString().toIntOrNull() ?: 0
            val newValue = if (maxValue != null) {
                minOf(currentVal + 1, maxValue)
            } else {
                currentVal + 1
            }
            editText.setText(newValue.toString())
            saveNumericValue(newValue, dataField)
        }

        // Minus button setup
        handleLongPress(editText, btnMinus, dataField, minValue, maxValue, false)
        btnMinus.setOnClickListener {
            val currentVal = editText.text.toString().toIntOrNull() ?: 0
            val newValue = maxOf(currentVal - 1, minValue)
            editText.setText(newValue.toString())
            saveNumericValue(newValue, dataField)
        }

        // 🚨 ADD TEXTWATCHER WITH INITIALIZATION CHECK
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 🛡️ GUARD: Skip if fragment is still initializing
                if (isFragmentInitializing || isUpdatingData) {
                    return
                }

                if (!s.isNullOrBlank()) {
                    try {
                        val enteredValue = s.toString().toInt()

                        val validatedValue = when {
                            enteredValue < minValue -> minValue
                            maxValue != null && enteredValue > maxValue -> maxValue
                            else -> enteredValue
                        }

                        if (enteredValue != validatedValue) {
                            editText.setText(validatedValue.toString())
                            editText.setSelection(editText.text.length)
                        }

                        saveNumericValue(validatedValue, dataField)
                    } catch (e: NumberFormatException) {
                        editText.setText(minValue.toString())
                        saveNumericValue(minValue, dataField)
                    }
                }
            }
        })

    }

    private fun saveNumericValue(value: Int, dataField: (PageData, Int) -> PageData) {
        val currentData = viewModel.getPageData(pageNumber) ?: PageData(pokokNumber = pageNumber)
        val updatedData = dataField(currentData, value)
        viewModel.savePageData(pageNumber, updatedData)
    }

    private fun updateDependentLayoutVisibility(selectedValue: Int) {
        val detailFormLayout = view?.findViewById<View>(R.id.lyDetailFormInspect)
        val jjgPanenLayout = view?.findViewById<View>(R.id.lyJjgPanenAKPInspect)

        val isInspection = viewModel.isInspection.value ?: true
        if (isInspection) {
            detailFormLayout?.visibility = if (selectedValue == 1) View.VISIBLE else View.GONE
            jjgPanenLayout?.visibility = View.GONE
        } else {
            detailFormLayout?.visibility = View.GONE
            jjgPanenLayout?.visibility = if (selectedValue == 1) View.VISIBLE else View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun handleLongPress(
        editText: EditText,
        button: CardView,
        dataField: (PageData, Int) -> PageData,
        minValue: Int = 0,
        maxValue: Int? = null,
        isIncrement: Boolean = true
    ) {
        var isLongPressing = false
        var runnableData: Runnable? = null
        val handler = Handler(Looper.getMainLooper())
        runnableData = Runnable {
            if (isLongPressing) {
                val currentVal = editText.text.toString().toIntOrNull() ?: 0
                val newValue = if (isIncrement) {
                    if (maxValue != null) {
                        minOf(currentVal + 1, maxValue)
                    } else {
                        currentVal + 1
                    }
                } else {
                    maxOf(currentVal - 1, minValue)
                }

                editText.setText(newValue.toString())
                saveNumericValue(newValue, dataField)

                if (isIncrement || newValue > minValue) {
                    val delay = if (isLongPressing) 100L else 300L
                    handler.postDelayed(runnableData!!, delay)
                } else {
                    isLongPressing = false
                }
            }
        }

        button.setOnLongClickListener {
            isLongPressing = true
            handler.post(runnableData)
            true
        }

        button.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                isLongPressing = false
            }
            false
        }
    }

    override fun onPause() {
        super.onPause()
        savePageData()
    }
}