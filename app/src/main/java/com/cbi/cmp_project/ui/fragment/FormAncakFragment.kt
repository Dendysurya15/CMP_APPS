package com.cbi.cmp_project.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
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
import androidx.lifecycle.ViewModelProvider
import com.cbi.cmp_project.ui.viewModel.FormAncakViewModel
import com.cbi.cmp_project.R
import com.cbi.cmp_project.ui.view.PanenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.cmp_project.ui.viewModel.FormAncakViewModel.*
import com.cbi.cmp_project.utils.AppLogger
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

    private val listRadioItems: Map<String, Map<String, String>> = mapOf(
        "YesOrNo" to mapOf(
            "1" to "Ya",
            "2" to "Tidak"
        ),
        "HighOrLow" to mapOf(
            "1" to "High",
            "2" to "Low"
        ),
        "ExistsOrNot" to mapOf(
            "1" to "Ada",
            "2" to "Tidak"
        ),
        "NeatOrNot" to mapOf(
            "1" to "Rapi",
            "2" to "Tidak Rapi"
        ),
        "PelepahType" to mapOf(
            "1" to "Alami",
            "2" to "Buatan",
            "3" to "Kering",
            "4" to "Tidak ada"
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

        val tvNoPokok = view.findViewById<TextView>(R.id.tvNoPokokInspect)
        tvNoPokok.text = pageNumber.toString()

        val tvTitleEst = view.findViewById<TextView>(R.id.tvTitleEstFormInspect)
        viewModel.estName.observe(viewLifecycleOwner) { est ->
            tvTitleEst.text = est ?: "-"
        }

        val itemListMapping = mapOf(
            R.id.lyPrioritasInspect to "HighOrLow",
            R.id.lyRatAttackInspect to "ExistsOrNot",
            R.id.lyGanoInspect to "ExistsOrNot",
            R.id.lyNeatPelepahInspect to "NeatOrNot",
            R.id.lyPelepahSengklehInspect to "PelepahType",
            R.id.lyPruningInspect to "PruningType",
        )

        val inputMappings: List<InputMapping> = listOf(
            InputMapping(
                R.id.lyExistsTreeInspect,
                "Titik Kosong?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(emptyTree = value) },
                { it.emptyTree }
            ),
            InputMapping(
                R.id.lyPrioritasInspect,
                "Prioritas?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(priority = value) },
                { it.priority }
            ),
            InputMapping(
                R.id.lyHarvestTreeInspect,
                "Pokok Dipanen?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(harvestTree = value) },
                { it.harvestTree }
            ),
            InputMapping(
                R.id.lyRatAttackInspect,
                "Serangan Tikus?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(ratAttack = value) },
                { it.ratAttack }
            ),
            InputMapping(
                R.id.lyGanoInspect,
                "Ganoderma?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(ganoderma = value) },
                { it.ganoderma }
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
                R.id.lyKentosanInspect,
                "Kentosan?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(kentosan = value) },
                { it.kentosan }
            ),
            InputMapping(
                R.id.lyBuahRipeInspect,
                "Buah Masak Tinggal di Pokok (S)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(ripe = value) },
                { it.ripe }
            ),
            InputMapping(
                R.id.lyBuahM1Inspect,
                "Buah Mentah Disembunyikan (M1)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(buahM1 = value) },
                { it.buahM1 }
            ),
            InputMapping(
                R.id.lyBuahM2Inspect,
                "Buah Matang Tidak Dikeluarkan (M2)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(buahM2 = value) },
                { it.buahM2 }
            ),
            InputMapping(
                R.id.lyBuahM3Inspect,
                "Buah Matahari (M3)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(buahM3 = value) },
                { it.buahM3 }
            ),
            InputMapping(
                R.id.lyBrdKtpInspect,
                "Brondolan Tidak dikutip",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdKtp = value) },
                { it.brdKtp }
            ),
            InputMapping(
                R.id.lyBrdInInspect,
                "Brondolan di dalam piringan (butir)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdIn = value) },
                { it.brdIn }
            ),
            InputMapping(
                R.id.lyBrdOutInspect,
                "Brondolan di luar piringan (butir)",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdOut = value) },
                { it.brdOut }
            ),
            InputMapping(
                R.id.lyPasarPikulInspect,
                "Pasar Pikul",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(pasarPikul = value) },
                { it.pasarPikul }
            ),
            InputMapping(
                R.id.lyKetiakInspect,
                "Ketiak",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(ketiak = value) },
                { it.ketiak }
            ),
            InputMapping(
                R.id.lyParitInspect,
                "Parit",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(parit = value) },
                { it.parit }
            ),
            InputMapping(
                R.id.lyBrdSegarInspect,
                "Brondolan Segar",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdSegar = value) },
                { it.brdSegar }
            ),
            InputMapping(
                R.id.lyBrdBusukInspect,
                "Brondolan Busuk",
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(brdBusuk = value) },
                { it.brdBusuk }
            ),
        )

        inputMappings.forEach { (layoutId, label, inputType, dataField, currentValue) ->
            when (inputType) {
                InputType.RADIO -> setupRadioGroup(
                    layoutId = layoutId,
                    titleText = label,
                    itemList = listRadioItems[itemListMapping[layoutId] ?: "YesOrNo"] ?: emptyMap(),
                    isRequired = false,
                    dataField = dataField,
                    currentValue = currentValue,
                )

                InputType.EDITTEXT -> setupNumericInput(
                    layoutId = layoutId,
                    titleText = label,
                    dataField = dataField,
                    currentValue = currentValue,
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
        isRequired: Boolean = true,
        dataField: (PageData, Int) -> PageData,
        currentValue: (PageData) -> Int
    ) {
        val layoutView = view?.findViewById<View>(layoutId) ?: return

        fun checkIsLayoutExistsTree(value: String) {
            val dependentLayout = view?.findViewById<View>(R.id.lyDetailFormInspect)
            val showDependentOn = "2" // Bukan titik kosong / janjang dipanen (AKP)

            if (layoutId == R.id.lyExistsTreeInspect) {
                dependentLayout?.visibility =
                    if (value == showDependentOn) View.VISIBLE else View.GONE
            }
        }

        val titleTextView = layoutView.findViewById<TextView>(R.id.tvTitleFormPanenTBS)
//        if (isRequired) {
//            val spannable = SpannableString("$questionTitle *")
//            spannable.setSpan(
//                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorRed)),
//                questionTitle.length, spannable.length,
//                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
//            )
//            titleTextView.text = spannable
//        } else {
        titleTextView.text = titleText
//        }

        val mcvSpinner = layoutView.findViewById<MaterialCardView>(R.id.MCVSpinner)
        val fblRadioComponents = layoutView.findViewById<FlexboxLayout>(R.id.fblRadioComponents)
        val errorTextView = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        mcvSpinner.visibility = View.GONE
        fblRadioComponents.visibility = View.VISIBLE

        val savedData = viewModel.getPageData(pageNumber) ?: PageData()
        fblRadioComponents.removeAllViews()

        var lastSelectedRadioButton: RadioButton? = null
        val fieldValue = currentValue(savedData)

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

                isChecked = idValue == fieldValue
                if (isChecked) {
                    lastSelectedRadioButton = this
                    checkIsLayoutExistsTree(id)
                }

                setOnClickListener {
                    checkIsLayoutExistsTree(id)
                    errorTextView.visibility = View.GONE
                    lastSelectedRadioButton?.isChecked = false
                    isChecked = true
                    lastSelectedRadioButton = this

                    val currentData = viewModel.getPageData(pageNumber) ?: PageData()
                    val updatedData = dataField(currentData, idValue)
                    viewModel.savePageData(pageNumber, updatedData)
                }
            }

            fblRadioComponents.addView(radioButton)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupNumericInput(
        layoutId: Int,
        titleText: String,
        dataField: (PageData, Int) -> PageData,
        currentValue: (PageData) -> Int,
        minValue: Int = 0,
        maxValue: Int? = null
    ) {
        val layoutView = view?.findViewById<View>(layoutId) ?: return

        val titleTextView = layoutView.findViewById<TextView>(R.id.tvNumberPanen)
        titleTextView.text = titleText

        val editText = layoutView.findViewById<EditText>(R.id.etNumber)
        val btnMinus = layoutView.findViewById<CardView>(R.id.btDec)
        val btnPlus = layoutView.findViewById<CardView>(R.id.btInc)

        val savedData = viewModel.getPageData(pageNumber) ?: PageData()
        val value = currentValue(savedData)

        editText.setText(value.toString())

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

        handleLongPress(editText, btnMinus, dataField, minValue, maxValue, false)
        btnMinus.setOnClickListener {
            val currentVal = editText.text.toString().toIntOrNull() ?: 0
            val newValue = maxOf(currentVal - 1, minValue)
            editText.setText(newValue.toString())
            saveNumericValue(newValue, dataField)
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
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

    private fun saveNumericValue(value: Int, dataField: (PageData, Int) -> PageData) {
        val currentData = viewModel.getPageData(pageNumber) ?: PageData()
        val updatedData = dataField(currentData, value)
        viewModel.savePageData(pageNumber, updatedData)

        AppLogger.d("Page $pageNumber: Updated numeric value to $value")
    }

    override fun onPause() {
        super.onPause()
        savePageData()
    }
}