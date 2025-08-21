package com.cbi.mobile_plantation.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.adapter.SelectedWorkerAdapter
import com.cbi.mobile_plantation.ui.adapter.Worker
import com.cbi.mobile_plantation.ui.view.panenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel
import com.cbi.mobile_plantation.ui.viewModel.FormAncakViewModel.PageData
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.card.MaterialCardView
import com.jaredrummler.materialspinner.MaterialSpinner

class FormAncakFragment : Fragment() {
    data class InputMapping(
        val layoudId: Int,
        val label: String,
        val inputType: InputType,
        val dataField: (PageData, Int) -> PageData,
        val currentValue: (PageData) -> Int
    )
    private lateinit var selectedPemanenTemuanAdapter: SelectedWorkerAdapter
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
            "1" to "Normal",
            "2" to "Over Pruning",
            "3" to "Under Pruning"
        )
    )

    private var pageNumber: Int = 1
    private var featureName: String? = null

    companion object {
        private const val ARG_PAGE_NUMBER = "page_number"
        private const val ARG_FEATURE_NAME = "feature_name"

        fun newInstance(pageNumber: Int, featureName: String?): FormAncakFragment {
            return FormAncakFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_PAGE_NUMBER, pageNumber)
                    putString(ARG_FEATURE_NAME, featureName)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pageNumber = it.getInt(ARG_PAGE_NUMBER, 1)
            featureName = it.getString(ARG_FEATURE_NAME)
        }

        AppLogger.d("Fragment created with featureName: $featureName, pageNumber: $pageNumber")

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

        setupPemanenTemuanRecyclerView()

        viewModel.formData.observe(viewLifecycleOwner) { formDataMap ->
            val pageData = formDataMap[pageNumber]
            if (pageData != null) {
                AppLogger.d("Fragment $pageNumber received pageData update: ${pageData.pemanen.size} workers in pemanen")

                // Populate RecyclerView based on pemanen field
                populateRecyclerViewFromPemanen(pageData.pemanen)
            }
        }

        setupAllInputs()

        // ⏰ Enable TextWatchers AFTER all setup is complete
        view.post {
            view.postDelayed({
                isFragmentInitializing = false
            }, 300)
        }
    }

    private fun populateRecyclerViewFromPemanen(pemanenMap: Map<String, String>) {
        if (pemanenMap.isEmpty()) {
            AppLogger.d("No pemanen data for page $pageNumber")
            selectedPemanenTemuanAdapter.clearAllWorkers()
            updateRecyclerViewVisibility()
            return
        }

        // Clear existing workers first
        selectedPemanenTemuanAdapter.clearAllWorkers()

        // Convert pemanen map to Worker objects and add to RecyclerView
        pemanenMap.forEach { (nik, name) ->
            val workerDisplayName = "$nik - $name"
            val worker = Worker(nik, workerDisplayName)

            // Add worker to RecyclerView
            selectedPemanenTemuanAdapter.addWorker(worker)

            AppLogger.d("Added worker to page $pageNumber RecyclerView: $workerDisplayName")
        }

        // Show RecyclerView since it now has items
        updateRecyclerViewVisibility()

        AppLogger.d("Populated RecyclerView for page $pageNumber with ${pemanenMap.size} workers from pemanen field")
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
            R.id.lyKondisiPruningInspect to "PruningType",
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
                R.id.lyHarvestTreeNumber,
                "", // You can change this title or leave empty ""
                InputType.EDITTEXT,
                { currentData, value -> currentData.copy(harvestJjg = value) }, // Assuming you have this field
                { it.harvestJjg } // Assuming you have this field
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
                R.id.lyKondisiPruningInspect,
                "Kondisi Pruning?",
                InputType.RADIO,
                { currentData, value -> currentData.copy(kondisiPruning = value) },
                { it.kondisiPruning }
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
            InputMapping(
                R.id.lyPemanenTemuan,
                "Pemanen",
                InputType.SPINNER,
                { currentData, value ->
                    // Note: This will be handled differently since it's a Map, not a single value
                    currentData // Return unchanged, actual handling in spinner selection
                },
                { it.pemanen.isEmpty().let { if (it) -1 else 0 } } // Return -1 if empty, 0 if has data
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

                InputType.SPINNER -> {
                    when (layoutId) {
                        R.id.lyPemanenTemuan -> {
                            setupPemanenSpinner(layoutId, label, currentPageData.pemanen)
                        }
                        else -> {
                            // Handle other spinners if any
                        }
                    }
                }

                else -> {}
            }
        }

        val harvestTreeValue = currentPageData.harvestTree
        updateHarvestTreeNumberVisibility(harvestTreeValue)
    }

    private fun setupPemanenTemuanRecyclerView() {
        val rvSelectedPemanenTemuan = view?.findViewById<RecyclerView>(R.id.rvSelectedPemanenTemuan) ?: return

        selectedPemanenTemuanAdapter = SelectedWorkerAdapter()
        rvSelectedPemanenTemuan.adapter = selectedPemanenTemuanAdapter
        rvSelectedPemanenTemuan.layoutManager = FlexboxLayoutManager(requireContext()).apply {
            justifyContent = JustifyContent.FLEX_START
        }

        // Set up the remove listener
        selectedPemanenTemuanAdapter.setOnWorkerActuallyRemovedListener { removedWorker ->
            AppLogger.d("Pemanen Temuan removal callback triggered for: ${removedWorker.name}")

            // Remove from page data
            removePemanenFromPageData(removedWorker)

            // Show RecyclerView if it has items, hide if empty
            updateRecyclerViewVisibility()
        }

        AppLogger.d("RecyclerView setup completed for page $pageNumber")
    }

    private fun populateRecyclerViewWithWorkers(availableWorkers: List<String>) {
        if (availableWorkers.isEmpty()) {
            AppLogger.d("No workers available for page $pageNumber")
            return
        }

        val currentPageData = viewModel.getPageData(pageNumber) ?: return

        // Convert available workers to Worker objects and add to RecyclerView
        availableWorkers.forEach { workerName ->
            // Extract NIK and name (format: "NIK - Name")
            val parts = workerName.split(" - ")
            if (parts.size >= 2) {
                val nik = parts[0].trim()
                val name = parts.subList(1, parts.size).joinToString(" - ").trim()

                // Check if this worker is already selected for this page
                if (!currentPageData.pemanen.containsKey(nik)) {
                    val worker = Worker(nik, workerName)

                    // Add worker to RecyclerView
                    selectedPemanenTemuanAdapter.addWorker(worker)

                    // Add to page data
                    addPemanenToPageData(nik, name)

                    AppLogger.d("Added worker to page $pageNumber: $workerName")
                }
            }
        }

        // Show RecyclerView since it now has items
        updateRecyclerViewVisibility()
    }



    private fun addPemanenToPageData(nik: String, name: String) {
        val currentPageData = viewModel.getPageData(pageNumber) ?: return

        // Add the pemanen to the map
        val updatedPemanen = currentPageData.pemanen.toMutableMap()
        updatedPemanen[nik] = name

        // Update the page data
        val updatedPageData = currentPageData.copy(pemanen = updatedPemanen)
        viewModel.updatePageData(pageNumber, updatedPageData)

        AppLogger.d("Added pemanen to page $pageNumber data: $nik -> $name")
    }


    private fun removePemanenFromPageData(removedWorker: Worker) {
        val currentPageData = viewModel.getPageData(pageNumber) ?: return

        // Extract NIK from worker
        val nik = removedWorker.id

        // Remove from page data
        val updatedPemanen = currentPageData.pemanen.toMutableMap()
        updatedPemanen.remove(nik)

        val updatedPageData = currentPageData.copy(pemanen = updatedPemanen)
        viewModel.updatePageData(pageNumber, updatedPageData)

        AppLogger.d("Removed pemanen from page $pageNumber data: ${removedWorker.name}")
    }

    private fun updateRecyclerViewVisibility() {
        val rvSelectedPemanenTemuan = view?.findViewById<RecyclerView>(R.id.rvSelectedPemanenTemuan)
        val selectedWorkers = selectedPemanenTemuanAdapter.getSelectedWorkers()

        rvSelectedPemanenTemuan?.visibility = if (selectedWorkers.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        AppLogger.d("RecyclerView visibility updated for page $pageNumber: ${selectedWorkers.size} workers")
    }

    private fun setupPemanenSpinner(
        layoutId: Int,
        titleText: String,
        selectedPemanen: Map<String, String>
    ) {
        val linearLayout = view?.findViewById<LinearLayout>(layoutId) ?: return
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val titleTextView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)

        titleTextView.text = titleText

        // Update hint to show current count
        val workerCount = selectedPemanen.size
        spinner.setHint("Pemanen Terpilih ($workerCount)")

        AppLogger.d("Pemanen spinner setup for page $pageNumber - showing $workerCount workers")
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

        // Check if this is follow-up inspection
        val isFollowUpInspection = featureName == AppUtils.ListFeatureNames.FollowUpInspeksi

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

            // Determine if this specific radio button should be disabled
            val shouldDisableThisButton = if (isFollowUpInspection) {
                if (layoutId == R.id.lyExistsTreeInspect) {
                    // For lyExistsTreeInspect: only disable if there's a selection and this isn't it
                    fieldValue != 0 && idValue != fieldValue
                } else {
                    // For other layouts: disable if this isn't the selected value
                    idValue != fieldValue
                }
            } else {
                false // Not follow-up inspection, don't disable
            }

            val radioButton = RadioButton(layoutView.context).apply {
                text = label
                tag = idValue
                textSize = 18f
                setTextColor(if (shouldDisableThisButton) Color.GRAY else Color.BLACK)
                setPadding(10, 0, 30, 0)
                buttonTintList = ContextCompat.getColorStateList(
                    layoutView.context,
                    if (shouldDisableThisButton) R.color.graydarker else R.color.greenDefault
                )
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                isChecked = idValue == fieldValue
                isEnabled = !shouldDisableThisButton

                if (isChecked) {
                    lastSelectedRadioButton = this
                }

                setOnClickListener {
                    // Skip click handling if disabled
                    if (shouldDisableThisButton) {
                        AppLogger.d("Radio button disabled for follow-up inspection")
                        return@setOnClickListener
                    }

                    clearValidationErrors()

                    lastSelectedRadioButton?.isChecked = false
                    isChecked = true
                    lastSelectedRadioButton = this

                    val currentData = viewModel.getPageData(pageNumber) ?: PageData(pokokNumber = pageNumber)
                    val updatedData = dataField(currentData, idValue)
                    viewModel.savePageData(pageNumber, updatedData)

                    if (layoutId == R.id.lyExistsTreeInspect) {
                        updateDependentLayoutVisibility(idValue)
                    }

                    if (layoutId == R.id.lyHarvestTreeInspect) {
                        updateHarvestTreeNumberVisibility(idValue)
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

    private fun updateHarvestTreeNumberVisibility(selectedValue: Int) {
        val harvestTreeNumberLayout = view?.findViewById<View>(R.id.lyHarvestTreeNumber)

        // Show lyHarvestTreeNumber only when "Ya" (value = 1) is selected
        harvestTreeNumberLayout?.visibility = if (selectedValue == 1) View.VISIBLE else View.GONE
    }



    fun updatePageData() {
        isUpdatingData = true

        setupAllInputs()

        val currentPageData = viewModel.getPageData(pageNumber)
        if (currentPageData != null) {
            updateHarvestTreeNumberVisibility(currentPageData.harvestTree)
            updateDependentLayoutVisibility(currentPageData.emptyTree)

            populateRecyclerViewFromPemanen(currentPageData.pemanen)
        }

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

        // Check if this is follow-up inspection
        val isFollowUpInspection = featureName == AppUtils.ListFeatureNames.FollowUpInspeksi

        titleTextView.text = titleText
        editText.setText(currentValue.toString())

        // Disable components for follow-up inspection
        editText.isEnabled = !isFollowUpInspection
        btnMinus.isEnabled = !isFollowUpInspection
        btnPlus.isEnabled = !isFollowUpInspection

        // Change visual appearance when disabled
        if (isFollowUpInspection) {
            editText.setTextColor(Color.BLACK)
            editText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.graylight))
            btnMinus.alpha = 0.5f
            btnPlus.alpha = 0.5f
        } else {
            editText.setTextColor(Color.BLACK)
            editText.setBackgroundColor(Color.WHITE)
            btnMinus.alpha = 1.0f
            btnPlus.alpha = 1.0f
        }

        // Plus button setup
        if (!isFollowUpInspection) {
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
        } else {
            btnPlus.setOnClickListener {
                AppLogger.d("Plus button disabled for follow-up inspection")
            }
        }

        // Minus button setup
        if (!isFollowUpInspection) {
            handleLongPress(editText, btnMinus, dataField, minValue, maxValue, false)
            btnMinus.setOnClickListener {
                val currentVal = editText.text.toString().toIntOrNull() ?: 0
                val newValue = maxOf(currentVal - 1, minValue)
                editText.setText(newValue.toString())
                saveNumericValue(newValue, dataField)
            }
        } else {
            btnMinus.setOnClickListener {
                AppLogger.d("Minus button disabled for follow-up inspection")
            }
        }

        // TextWatcher setup
        if (!isFollowUpInspection) {
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
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