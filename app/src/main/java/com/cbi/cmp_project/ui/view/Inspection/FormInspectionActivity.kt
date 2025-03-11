package com.cbi.cmp_project.ui.view.Inspection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.repository.CameraRepository
import com.cbi.cmp_project.ui.adapter.FormAncakPagerAdapter
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter
import com.cbi.cmp_project.ui.adapter.TakeFotoPreviewAdapter.Companion.CAMERA_PERMISSION_REQUEST_CODE
import com.cbi.cmp_project.ui.fragment.FormAncakFragment
import com.cbi.cmp_project.ui.view.HomePageActivity
import com.cbi.cmp_project.ui.view.PanenTBS.FeaturePanenTBSActivity.InputType
import com.cbi.cmp_project.ui.viewModel.CameraViewModel
import com.cbi.cmp_project.ui.viewModel.FormAncakViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.AppUtils.vibrate
import com.cbi.cmp_project.utils.LoadingDialog
import com.cbi.cmp_project.utils.PrefManager
import com.cbi.cmp_project.utils.SoftKeyboardStateWatcher
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.jaredrummler.materialspinner.MaterialSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

class FormInspectionActivity : AppCompatActivity(), CameraRepository.PhotoCallback {
    private lateinit var loadingDialog: LoadingDialog
    private var prefManager: PrefManager? = null

    private var featureName: String? = null
    private var regionalId: String? = null
    private var estateId: String? = null
    private var estateName: String? = null
    private var userName: String? = null
    private var userId: Int? = null
    private var jabatanUser: String? = null

    private var jumBrdTglPath = 0
    private var jumBuahTglPath = 0

    private var shouldReopenBottomSheet = false

    private val listRadioItems: Map<String, Map<String, String>> = mapOf(
        "InspectionType" to mapOf(
            "1" to "Inspeksi",
            "2" to "AKP"
        ),
        "ConditionType" to mapOf(
            "1" to "Datar",
            "2" to "Teras"
        )
    )

    private lateinit var inputMappings: List<Triple<LinearLayout, String, InputType>>

    private lateinit var cameraViewModel: CameraViewModel
    private lateinit var formAncakViewModel: FormAncakViewModel
    private lateinit var formAncakPagerAdapter: FormAncakPagerAdapter
    private lateinit var keyboardWatcher: SoftKeyboardStateWatcher

    private lateinit var infoBlokView: ScrollView
    private lateinit var formInspectionView: ConstraintLayout
    private lateinit var bottomNavInspect: BottomNavigationView
    private lateinit var vpFormAncak: ViewPager2
    private lateinit var fabPrevFormAncak: FloatingActionButton
    private lateinit var fabNextFormAncak: FloatingActionButton
    private lateinit var fabPhotoFormAncak: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form_inspection)

        loadingDialog = LoadingDialog(this)
        prefManager = PrefManager(this)

        initViewModel()

        regionalId = prefManager!!.regionalIdUserLogin
        estateId = prefManager!!.estateIdUserLogin
        estateName = prefManager!!.estateUserLogin
        userName = prefManager!!.nameUserLogin
        userId = prefManager!!.idUserLogin
        jabatanUser = prefManager!!.jabatanUserLogin

        val backButton = findViewById<ImageView>(R.id.btn_back)
        backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        setupHeader()

        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                loadingDialog.show()
                loadingDialog.setMessage("Loading data...")
                delay(1000)
            }

            try {
                withContext(Dispatchers.Main) {
                    setupLayout()
                    setKeyboardVisibilityListener()
                    loadingDialog.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = e.message?.let { "1. $it" } ?: "1. Unknown error"

                    val estateInfo = estateId?.takeIf { it.isBlank() }
                        ?.let { "2. ID Estate User Login: \"$it\"" }

                    val fullMessage = listOfNotNull(errorMessage, estateInfo).joinToString("\n\n")

                    AppLogger.e("Error fetching data: ${e.message}")

                    AlertDialogUtility.withSingleAction(
                        this@FormInspectionActivity,
                        stringXML(R.string.al_back),
                        stringXML(R.string.al_failed_fetch_data),
                        fullMessage,
                        "warning.json",
                        R.color.colorRedDark
                    ) {
                        finish()
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    cameraViewModel.statusCamera() -> {
                        cameraViewModel.closeCamera()

                        if (shouldReopenBottomSheet) {
                            shouldReopenBottomSheet = false
                            Handler(Looper.getMainLooper()).postDelayed({
                                showViewPhotoBottomSheet()
                            }, 100)
                        }
                    }

                    else -> {
                        vibrate()
                        AlertDialogUtility.withTwoActions(
                            this@FormInspectionActivity,
                            "Keluar",
                            getString(R.string.confirmation_dialog_title),
                            getString(R.string.al_confirm_feature),
                            "warning.json",
                            ContextCompat.getColor(
                                this@FormInspectionActivity,
                                R.color.bluedarklight
                            )
                        ) {
                            val intent =
                                Intent(this@FormInspectionActivity, HomePageActivity::class.java)
                            startActivity(intent)
                            finishAffinity()
                        }

                    }
                }
            }
        })
    }

    override fun onDestroy() {
        keyboardWatcher.unregister()
        super.onDestroy()
    }

    private fun initViewModel() {
        formAncakViewModel = ViewModelProvider(this)[FormAncakViewModel::class.java]

        val idTakeFotoLayout = findViewById<View>(R.id.incTakePhotoInspect)
        val idEditFotoLayout = findViewById<View>(R.id.incEditPhotoInspect)
        val cameraRepository = CameraRepository(this, window, idTakeFotoLayout, idEditFotoLayout)
        cameraRepository.setPhotoCallback(this)
        cameraViewModel = ViewModelProvider(
            this,
            CameraViewModel.Factory(cameraRepository)
        )[CameraViewModel::class.java]
    }

    private fun setupViewPager() {
        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        formAncakPagerAdapter = FormAncakPagerAdapter(this, totalPages)

        vpFormAncak.apply {
            adapter = formAncakPagerAdapter
            isUserInputEnabled = false
            setPageTransformer(createPageTransformer())
        }

        Handler(Looper.getMainLooper()).post {
            preloadAllPages()
        }
    }

    private fun createPageTransformer(): ViewPager2.PageTransformer {
        return ViewPager2.PageTransformer { page, position ->
            val absPosition = abs(position)
            page.alpha = 0.8f.coerceAtLeast(1 - absPosition)

            val scale = 0.95f.coerceAtLeast(1 - absPosition * 0.05f)
            page.scaleX = scale
            page.scaleY = scale
            page.translationX = page.width * (if (position < 0) -0.02f else 0.02f) * position
        }
    }

    private fun preloadAllPages() {
        val totalPages = formAncakViewModel.totalPages.value ?: 10
        for (i in 0 until totalPages) {
            vpFormAncak.setCurrentItem(i, false)
        }

        vpFormAncak.setCurrentItem(0, false)
        vpFormAncak.requestLayout()
        vpFormAncak.invalidate()
    }

    private fun observeViewModel() {
        formAncakViewModel.currentPage.observe(this) { page ->
            val pageIndex = page - 1

            if (vpFormAncak.currentItem != pageIndex) {
                vpFormAncak.setCurrentItem(pageIndex, true)
            }

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val totalPages =
                formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION

            Handler(Looper.getMainLooper()).postDelayed({
                fabPrevFormAncak.isEnabled = if (currentPage <= 1) false else true
                fabPrevFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage <= 1) R.color.greytext else androidx.biometric.R.color.biometric_error_color
                    )
                )
                fabNextFormAncak.isEnabled = if (currentPage >= totalPages) false else true
                fabNextFormAncak.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        if (currentPage >= totalPages) R.color.greytext else R.color.greenDefault
                    )
                )
            }, 300)
        }

        formAncakViewModel.formData.observe(this) { formData ->
            updatePhotoBadgeVisibility()
        }

        formAncakViewModel.fieldValidationError.observe(this) { errorMap ->
            if (errorMap.isNotEmpty()) {
                val currentFragment =
                    supportFragmentManager.findFragmentByTag("f${vpFormAncak.currentItem}")
                if (currentFragment is FormAncakFragment) {
                    errorMap.forEach { (fieldId, errorMessage) ->
                        currentFragment.run { showValidationError(fieldId, errorMessage) }
                    }
                }
            }
        }
    }

    private fun setupNavigation() {
        fabNextFormAncak.setOnClickListener {
            val validationResult = formAncakViewModel.validateCurrentPage()
            if (validationResult.isValid) {
                val currentPage = formAncakViewModel.currentPage.value ?: 1
                val nextPage = currentPage + 1
                val totalPages =
                    formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION

                logAllPagesEmptyTreeStatus()

                if (nextPage <= totalPages) {
                    lifecycleScope.launch {
                        withContext(Dispatchers.Main) {
                            loadingDialog.show()
                            loadingDialog.setMessage("Loading data...")

                            vpFormAncak.post {
                                vpFormAncak.setCurrentItem(nextPage - 1, false)
                                vpFormAncak.setCurrentItem(currentPage - 1, false)

                                Handler(Looper.getMainLooper()).post {
                                    val pageChangeCallback = createPageChangeCallback()
                                    vpFormAncak.registerOnPageChangeCallback(pageChangeCallback)

                                    formAncakViewModel.nextPage()

                                    Handler(Looper.getMainLooper()).postDelayed({
                                        if (loadingDialog.isShowing) {
                                            scrollToTopOfFormAncak()
                                            loadingDialog.dismiss()
                                            vpFormAncak.unregisterOnPageChangeCallback(
                                                pageChangeCallback
                                            )
                                        }
                                    }, 500)
                                }
                            }
                        }
                    }
                }
            } else {
                vibrate(500)
            }
        }

        fabPrevFormAncak.setOnClickListener {
            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val prevPage = currentPage - 1

            if (prevPage >= 1) {
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        loadingDialog.show()
                        loadingDialog.setMessage("Loading data...")

                        vpFormAncak.post {
                            vpFormAncak.setCurrentItem(prevPage - 1, false)
                            vpFormAncak.setCurrentItem(currentPage - 1, false)

                            Handler(Looper.getMainLooper()).post {
                                val pageChangeCallback = createPageChangeCallback()
                                vpFormAncak.registerOnPageChangeCallback(pageChangeCallback)

                                formAncakViewModel.previousPage()

                                Handler(Looper.getMainLooper()).postDelayed({
                                    if (loadingDialog.isShowing) {
                                        loadingDialog.dismiss()
                                        vpFormAncak.unregisterOnPageChangeCallback(
                                            pageChangeCallback
                                        )
                                    }
                                }, 500)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun logAllPagesEmptyTreeStatus() {
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val totalPages = formAncakViewModel.totalPages.value ?: AppUtils.TOTAL_MAX_TREES_INSPECTION
        val formData = formAncakViewModel.formData.value ?: mutableMapOf()

        AppLogger.d("--- EMPTY TREE STATUS FOR ALL PAGES ---")

        for (page in 1..totalPages) {
            val pageData = formData[page]
            val emptyTreeValue = pageData?.emptyTree ?: 0
            val photoValue = pageData?.photo ?: 0
            val commentValue = pageData?.comment ?: 0

            AppLogger.d("Page $page: EmptyTree = $emptyTreeValue")
            AppLogger.d("Page $page: Photo = $photoValue")
            AppLogger.d("Page $page: Comment = $commentValue")

            if (page == currentPage) break
        }

        AppLogger.d("--- END OF EMPTY TREE STATUS ---")
    }

    private fun createPageChangeCallback(): ViewPager2.OnPageChangeCallback {
        return object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    scrollToTopOfFormAncak()
                    loadingDialog.dismiss()
                    vpFormAncak.unregisterOnPageChangeCallback(this)
                }
            }
        }
    }

    // Function to force scroll to top of FormAncakFragment
    private fun scrollToTopOfFormAncak() {
        val fragmentIndex = vpFormAncak.currentItem
        val fragmentTag = "f$fragmentIndex"

        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment is FormAncakFragment) {
            fragment.scrollToTop()
        }
    }

    private fun setupAddPhotosView() {
        fabPhotoFormAncak.setOnClickListener {
            val validationResult = formAncakViewModel.validateCurrentPage()
            if (validationResult.isValid) {
                showViewPhotoBottomSheet()
            } else {
                vibrate(500)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showViewPhotoBottomSheet(fileName: String? = null) {
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val currentData =
            formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()
        val rootApp = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()

        val bottomSheetDialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_inspection_photo, null)
        bottomSheetDialog.setContentView(view)

        view.background = ContextCompat.getDrawable(this, R.drawable.rounded_top_right_left)

        val ibDeletePhotoInspect = view.findViewById<ImageButton>(R.id.ibDeletePhotoInspect)
        val incLytPhotosInspect = view.findViewById<View>(R.id.incLytPhotosInspect)
        val ivAddPhoto = incLytPhotosInspect.findViewById<ImageView>(R.id.ivAddFoto)
        val tvPhotoComment = incLytPhotosInspect.findViewById<TextView>(R.id.tvPhotoComment)
        val etPhotoComment = incLytPhotosInspect.findViewById<EditText>(R.id.etPhotoComment)

        tvPhotoComment.visibility = View.GONE

        ibDeletePhotoInspect.visibility = if (currentData.photo.isNotEmpty() || currentData.comment.isNotEmpty()) View.VISIBLE else View.GONE
        ibDeletePhotoInspect.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                this,
                "Hapus",
                this.getString(R.string.confirmation_dialog_title),
                "Apakah anda yakin untuk menghapus lampiran ini?",
                "warning.json",
                ContextCompat.getColor(this, R.color.greenDarker)
            ) {
                ivAddPhoto.setImageResource(R.drawable.baseline_add_a_photo_24)
                etPhotoComment.setText("")

                formAncakViewModel.savePageData(
                    currentPage,
                    currentData.copy(photo = "", comment = "")
                )

                ibDeletePhotoInspect.visibility = View.GONE
                updatePhotoBadgeVisibility()
                etPhotoComment.clearFocus()
            }
        }

        etPhotoComment.visibility = View.VISIBLE
        etPhotoComment.setText(currentData.comment)
        etPhotoComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {
                formAncakViewModel.savePageData(
                    currentPage,
                    currentData.copy(comment = s?.toString() ?: "")
                )
            }
        })

        var resultFileName = currentData.photo
        if (fileName != null) {
            resultFileName = fileName
        }

        ivAddPhoto.setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    bottomNavInspect.visibility = View.GONE
                    bottomSheetDialog.dismiss()

                    if (resultFileName.isNotEmpty()) {
                        val filePath = File(File(rootApp, "CMP"), resultFileName)
                        Handler(Looper.getMainLooper()).postDelayed({
                            cameraViewModel.openZoomPhotos(
                                file = filePath,
                                position = currentPage.toString(),
                                onChangePhoto = {
                                    shouldReopenBottomSheet = true
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        cameraViewModel.takeCameraPhotos(
                                            currentPage.toString(),
                                            ivAddPhoto,
                                            currentPage,
                                            null,
                                            "", // soon assign lat lon
                                            currentPage.toString(),
                                            "IP"
                                        )
                                    }, 100)
                                },
                                onDeletePhoto = { pos ->
                                    ivAddPhoto.setImageResource(R.drawable.baseline_add_a_photo_24)
                                    formAncakViewModel.savePageData(
                                        currentPage,
                                        currentData.copy(photo = "")
                                    )
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showViewPhotoBottomSheet()
                                    }, 100)
                                },
                                onClosePhoto = {
                                    bottomNavInspect.visibility = View.VISIBLE
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        showViewPhotoBottomSheet()
                                    }, 100)
                                }
                            )
                        }, 100)
                    } else {
                        shouldReopenBottomSheet = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            cameraViewModel.takeCameraPhotos(
                                currentPage.toString(),
                                ivAddPhoto,
                                currentPage,
                                null,
                                "", // soon assign lat lon
                                currentPage.toString(),
                                "IP"
                            )
                        }, 100)
                    }
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.CAMERA
                ) -> {
                    showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
                }

                else -> {
                    // If permission is permanently denied, show settings option
                    if (isPermissionPermanentlyDenied()) {
                        AppLogger.d("Permission permanently denied. Redirecting to settings.")
                        showSnackbarWithSettings("Camera permission required to take photos. Enable it in Settings.")
                    } else {
                        AppLogger.d("Requesting camera permission")
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(android.Manifest.permission.CAMERA),
                            CAMERA_PERMISSION_REQUEST_CODE
                        )
                    }
                }
            }
        }

        if (resultFileName.isNotEmpty()) {
            val filePath = File(File(rootApp, "CMP"), resultFileName)
            Glide.with(this)
                .load(filePath)
                .into(ivAddPhoto)
        }

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels

        bottomSheetDialog.window?.apply {
            setLayout(
                (width * 0.8).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        bottomSheetDialog.setOnShowListener {
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        bottomSheetDialog.show()
    }

    private fun setKeyboardVisibilityListener() {
        fun showWithAnimation(view: View) {
            view.apply {
                if (visibility != View.VISIBLE) {
                    visibility = View.VISIBLE
                    translationY = 100f
                    alpha = 0f
                    animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(300)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        }

        val rootView = findViewById<View>(android.R.id.content)
        keyboardWatcher = SoftKeyboardStateWatcher(
            rootView,
            object : SoftKeyboardStateWatcher.OnSoftKeyboardStateChangedListener {
                override fun onSoftKeyboardOpened(keyboardHeight: Int) {
                    bottomNavInspect.visibility = View.GONE
                    fabPrevFormAncak.visibility = View.GONE
                    fabNextFormAncak.visibility = View.GONE
                    fabPhotoFormAncak.visibility = View.GONE
                }

                override fun onSoftKeyboardClosed() {
                    bottomNavInspect.post {
                        showWithAnimation(bottomNavInspect)
                        showWithAnimation(fabPrevFormAncak)
                        showWithAnimation(fabNextFormAncak)
                        showWithAnimation(fabPhotoFormAncak)
                    }
                }
            })
    }

    private fun setupHeader() {
        featureName = intent.getStringExtra("FEATURE_NAME")
        val tvFeatureName = findViewById<TextView>(R.id.tvFeatureName)
        val userSection = findViewById<TextView>(R.id.userSection)
        val locationSection = findViewById<LinearLayout>(R.id.locationSection)
        locationSection.visibility = View.VISIBLE

        AppUtils.setupUserHeader(
            userName = userName,
            jabatanUser = jabatanUser,
            estateName = estateName,
            afdelingUser = "",
            userSection = userSection,
            featureName = featureName,
            tvFeatureName = tvFeatureName
        )
    }

    private fun setupLayout() {
        infoBlokView = findViewById(R.id.svInfoBlokInspection)
        formInspectionView = findViewById(R.id.clFormInspection)
        bottomNavInspect = findViewById(R.id.bottomNavInspect)
        vpFormAncak = findViewById(R.id.vpFormAncakInspect)
        fabPrevFormAncak = findViewById(R.id.fabPrevFormInspect)
        fabNextFormAncak = findViewById(R.id.fabNextFormInspect)
        fabPhotoFormAncak = findViewById(R.id.fabPhotoFormInspect)

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                setupViewPager()
                setupNavigation()
                setupAddPhotosView()
                observeViewModel()
            }
        }

        bottomNavInspect.setOnItemSelectedListener { item ->
            val activeBottomNavId = bottomNavInspect.selectedItemId
            if (activeBottomNavId == item.itemId) return@setOnItemSelectedListener false

            val validationResult = formAncakViewModel.validateCurrentPage()
            if (activeBottomNavId == R.id.navMenuAncakInspect && !validationResult.isValid) {
                vibrate(500)
                return@setOnItemSelectedListener false
            }

            loadingDialog.show()
            loadingDialog.setMessage("Loading data...")

            lifecycleScope.launch {
                when (item.itemId) {
                    R.id.navMenuBlokInspect -> {
                        withContext(Dispatchers.Main) {
                            infoBlokView.visibility = View.VISIBLE
                            formInspectionView.visibility = View.GONE
                            delay(200)
                            loadingDialog.dismiss()
                        }
                    }

                    R.id.navMenuAncakInspect -> {
                        withContext(Dispatchers.Main) {
                            formAncakViewModel.updateEstName("UPE")

                            infoBlokView.visibility = View.GONE
                            formInspectionView.post {
                                vpFormAncak.post {
                                    formInspectionView.visibility = View.VISIBLE
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        loadingDialog.dismiss()
                                    }, 300)
                                }
                            }
                        }
                    }
                }
            }

            return@setOnItemSelectedListener when (item.itemId) {
                R.id.navMenuBlokInspect, R.id.navMenuAncakInspect -> true
                else -> false
            }
        }
        bottomNavInspect.selectedItemId = R.id.navMenuBlokInspect

        inputMappings = listOf(
            Triple(
                findViewById(R.id.lyEstInspect),
                getString(R.string.field_estate),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyAfdInspect),
                getString(R.string.field_afdeling),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyTtInspect),
                getString(R.string.field_tahun_tanam),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyBlokInspect),
                getString(R.string.field_blok),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyAncakInspect),
                getString(R.string.field_ancak),
                InputType.EDITTEXT
            ),
            Triple(
                findViewById(R.id.lyNoTphInspect),
                getString(R.string.field_no_tph),
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyStatusPanenInspect),
                "Status Panen",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyJalurInspect),
                "Jalur Masuk",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyInspectionType),
                "Jenis Inspeksi",
                InputType.RADIO
            ),
            Triple(
                findViewById(R.id.lyMandor1Inspect),
                "Kemandoran",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyPemanen1Inspect),
                "Pemanen",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyMandor2Inspect),
                "Kemandoran Lain",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyPemanen2Inspect),
                "Pemanen Lain",
                InputType.SPINNER
            ),
            Triple(
                findViewById(R.id.lyConditionType),
                "Jenis Kondisi",
                InputType.RADIO
            ),
            Triple(
                findViewById(R.id.lyBaris1Inspect),
                "Baris Pertama",
                InputType.EDITTEXT
            ),
            Triple(
                findViewById(R.id.lyBaris2Inspect),
                "Baris Kedua",
                InputType.EDITTEXT
            ),
        )

        inputMappings.forEach { (layoutView, key, inputType) ->
            updateLabelTextView(layoutView, key)
            when (inputType) {
                InputType.SPINNER -> {
                    when (layoutView.id) {
                        else -> setupSpinnerView(layoutView, emptyList())
                    }
                }

                InputType.EDITTEXT -> setupEditTextView(layoutView)
                InputType.RADIO -> {
                    when (layoutView.id) {
                        R.id.lyInspectionType -> setupRadioView(
                            layoutView,
                            listRadioItems["InspectionType"] ?: emptyMap()
                        )

                        R.id.lyConditionType -> setupRadioView(
                            layoutView,
                            listRadioItems["ConditionType"] ?: emptyMap()
                        )
                    }
                }
            }
        }

        val counterMappings = listOf(
            Triple(R.id.lyBrdTglInspect, "Brondolan Tinggal", ::jumBrdTglPath),
            Triple(R.id.lyBuahTglInspect, "Buah Tinggal", ::jumBuahTglPath),
        )
        counterMappings.forEach { (layoutId, labelText, counterVar) ->
            setupPaneWithButtons(layoutId, R.id.tvNumberPanen, labelText, counterVar)
        }
    }

    private fun updateLabelTextView(linearLayout: LinearLayout, text: String) {
        val textView = linearLayout.findViewById<TextView>(R.id.tvTitleFormPanenTBS)

        val spannable = SpannableString("$text *")
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(this, R.color.colorRed)),
            text.length, spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        textView.text = spannable
    }

    private fun updatePhotoBadgeVisibility() {
        val currentPage = formAncakViewModel.currentPage.value ?: 1
        val currentData =
            formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()

        val badgePhotoInspect = findViewById<View>(R.id.badgePhotoInspect)
        badgePhotoInspect.visibility = if (currentData.photo.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSpinnerView(
        linearLayout: LinearLayout,
        data: List<String>,
        onItemSelected: (Int) -> Unit = {}
    ) {
        val editText = linearLayout.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spinner = linearLayout.findViewById<MaterialSpinner>(R.id.spPanenTBS)
        val tvError = linearLayout.findViewById<TextView>(R.id.tvErrorFormPanenTBS)

        spinner.setItems(data)

        if (linearLayout.id == R.id.layoutKemandoran || linearLayout.id == R.id.layoutPemanen || linearLayout.id == R.id.layoutKemandoranLain || linearLayout.id == R.id.layoutPemanenLain) {
            spinner.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    showPopupSearchDropdown(
                        spinner,
                        data,
                        editText,
                        linearLayout
                    ) { selectedItem, position ->
                        spinner.text = selectedItem
                        tvError.visibility = View.GONE
                        onItemSelected(position)
                    }
                }
                true
            }
        }


        if (linearLayout.id == R.id.layoutEstate) {
            spinner.isEnabled = false // Disable spinner
        }

        spinner.setOnItemSelectedListener { _, position, _, item ->
            tvError.visibility = View.GONE
//            handleItemSelection(linearLayout, position, item.toString())
        }
    }

    private fun setupEditTextView(layoutView: LinearLayout) {
        val etHomeMarkerTPH = layoutView.findViewById<EditText>(R.id.etHomeMarkerTPH)
        val spHomeMarkerTPH = layoutView.findViewById<View>(R.id.spPanenTBS)
        val tvError = layoutView.findViewById<TextView>(R.id.tvErrorFormPanenTBS)
        val MCVSpinner = layoutView.findViewById<View>(R.id.MCVSpinner)

        spHomeMarkerTPH.visibility = View.GONE
        etHomeMarkerTPH.visibility = View.VISIBLE

        // Set input type based on layout ID
        etHomeMarkerTPH.inputType = when (layoutView.id) {
            R.id.layoutAncak -> android.text.InputType.TYPE_CLASS_NUMBER
            else -> android.text.InputType.TYPE_CLASS_TEXT
        }

        etHomeMarkerTPH.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm =
                    application.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                findViewById<MaterialSpinner>(R.id.spPanenTBS)?.requestFocus()
                true
            } else {
                false
            }
        }

        etHomeMarkerTPH.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvError.visibility = View.GONE
                MCVSpinner.setBackgroundColor(
                    ContextCompat.getColor(
                        layoutView.context,
                        R.color.graytextdark
                    )
                )

//                if (layoutView.id == R.id.layoutAncak) {
//                    ancakInput = s?.toString()?.trim() ?: ""
//                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRadioView(
        layoutView: LinearLayout,
        itemList: Map<String, String>
    ) {
        val mcvSpinner = layoutView.findViewById<MaterialCardView>(R.id.MCVSpinner)
        val fblRadioComponents = layoutView.findViewById<FlexboxLayout>(R.id.fblRadioComponents)

        mcvSpinner.visibility = View.GONE
        fblRadioComponents.visibility = View.VISIBLE

        var lastSelectedRadioButton: RadioButton? = null
        val isFirstIndex = itemList.entries.firstOrNull()
        itemList.forEach { (id, label) ->
            val radioButton = RadioButton(layoutView.context).apply {
                text = label
                tag = View.generateViewId()
                textSize = 18f
                setTextColor(Color.BLACK)
                setPadding(10, 0, 30, 0)
                buttonTintList = getColorStateList(R.color.greenDefault)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                if (id == isFirstIndex?.key) {
                    isChecked = true
                    lastSelectedRadioButton = this
                }

                setOnClickListener {
                    lastSelectedRadioButton?.isChecked = false
                    isChecked = true
                    lastSelectedRadioButton = this
                }
            }

            fblRadioComponents.addView(radioButton)
        }
    }

    private fun setupPaneWithButtons(
        layoutId: Int,
        textViewId: Int,
        labelText: String,
        counterVar: KMutableProperty0<Int>
    ) {
        val includedLayout = findViewById<View>(layoutId)
        val textView = includedLayout.findViewById<TextView>(textViewId)
        val etNumber = includedLayout.findViewById<EditText>(R.id.etNumber)
        val tvPercent = includedLayout.findViewById<TextView>(R.id.tvPercent)

        textView.text = labelText
        etNumber.setText(counterVar.get().toString())

        etNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                etNumber.removeTextChangedListener(this)

                try {
                    val newValue = if (s.isNullOrEmpty()) 0 else s.toString().toInt()

                } catch (e: NumberFormatException) {
                    etNumber.setText(counterVar.get().toString())
                    vibrate()
                }

                etNumber.addTextChangedListener(this)
            }
        })

        val btDec = includedLayout.findViewById<CardView>(R.id.btDec)
        val btInc = includedLayout.findViewById<CardView>(R.id.btInc)
        fun vibrate() {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    50,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }

//        btDec.setOnClickListener {
//            if (counterVar.get() > 0) {
//                updateDependentCounters(
//                    layoutId,
//                    -1,
//                    counterVar,
//                    tvPercent
//                )  // Decrement through dependent counter
//                etNumber.setText(counterVar.get().toString())
//            } else {
//                vibrate()
//            }
//        }
//
//        btInc.setOnClickListener {
//            updateDependentCounters(
//                layoutId,
//                1,
//                counterVar,
//                tvPercent
//            )  // Increment through dependent counter
//            etNumber.setText(counterVar.get().toString())
//        }
    }

    private fun showPopupSearchDropdown(
        spinner: MaterialSpinner,
        data: List<String>,
        editText: EditText,
        linearLayout: LinearLayout,
        onItemSelected: (String, Int) -> Unit
    ) {
        val popupView =
            LayoutInflater.from(spinner.context).inflate(R.layout.layout_dropdown_search, null)
        val listView = popupView.findViewById<ListView>(R.id.listViewChoices)
        val editTextSearch = popupView.findViewById<EditText>(R.id.searchEditText)

        val scrollView = findScrollView(linearLayout)
        val rootView = linearLayout.rootView

        // Create PopupWindow first
        val popupWindow = PopupWindow(
            popupView,
            spinner.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isFocusable = true
            isOutsideTouchable = true
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        var keyboardHeight = 0
        val rootViewLayout = rootView.viewTreeObserver
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height

            // Get keyboard height
            val newKeyboardHeight = screenHeight - rect.bottom

            if (newKeyboardHeight != keyboardHeight) {
                keyboardHeight = newKeyboardHeight
                if (keyboardHeight > 0) {
                    val spinnerLocation = IntArray(2)
                    spinner.getLocationOnScreen(spinnerLocation)

                    if (spinnerLocation[1] + spinner.height + popupWindow.height > rect.bottom) {
                        val scrollAmount = spinnerLocation[1] - 400
                        scrollView?.smoothScrollBy(0, scrollAmount)
                    }
                }
            }
        }

        rootViewLayout.addOnGlobalLayoutListener(layoutListener)

        popupWindow.setOnDismissListener {
            rootViewLayout.removeOnGlobalLayoutListener(layoutListener)
        }

        var filteredData = data
        val adapter = object : ArrayAdapter<String>(
            spinner.context,
            android.R.layout.simple_list_item_1,
            filteredData
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK)
                return view
            }
        }
        listView.adapter = adapter

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val titleSearch = popupView.findViewById<TextView>(R.id.titleSearchDropdown)

                filteredData = if (!s.isNullOrEmpty()) {
                    titleSearch.visibility = View.VISIBLE
                    data.filter { it.contains(s, ignoreCase = true) }
                } else {
                    titleSearch.visibility = View.GONE
                    data
                }

                val filteredAdapter = object : ArrayAdapter<String>(
                    spinner.context,
                    android.R.layout.simple_list_item_1,
                    if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                        listOf("Data tidak tersedia!")
                    } else {
                        filteredData
                    }
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)

                        if (filteredData.isEmpty() && !s.isNullOrEmpty()) {
                            textView.setTextColor(
                                ContextCompat.getColor(
                                    context,
                                    R.color.colorRedDark
                                )
                            )
                            textView.setTypeface(textView.typeface, Typeface.ITALIC)
                            view.isEnabled = false
                        } else {
                            textView.setTextColor(Color.BLACK)
                            textView.setTypeface(textView.typeface, Typeface.NORMAL)
                            view.isEnabled = true
                        }
                        return view
                    }

                    override fun isEnabled(position: Int): Boolean {
                        return filteredData.isNotEmpty()
                    }
                }
                listView.adapter = filteredAdapter
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = filteredData[position]
            spinner.text = selectedItem
            editText.setText(selectedItem)
//            handleItemSelection(linearLayout, position, selectedItem)
            popupWindow.dismiss()
        }

        popupWindow.showAsDropDown(spinner)

        editTextSearch.requestFocus()
        val imm =
            spinner.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editTextSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun findScrollView(view: View): ScrollView? {
        var parent = view.parent
        while (parent != null) {
            if (parent is ScrollView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }

    private fun isPermissionPermanentlyDenied(): Boolean {
        val sharedPref = this.getSharedPreferences("permissions_prefs", Context.MODE_PRIVATE)
        val firstRequest = sharedPref.getBoolean("first_camera_request", true)

        if (firstRequest) {
            sharedPref.edit().putBoolean("first_camera_request", false).apply()
            return false
        }

        return !ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)
    }

    private fun showSnackbarWithSettings(message: String) {
        Snackbar.make(this.findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE)
            .setAction("Settings") {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", this.packageName, null)
                )
                this.startActivity(intent)
            }
            .show()
    }

    override fun onPhotoTaken(
        photoFile: File,
        fname: String,
        resultCode: String,
        deletePhoto: View?,
        position: Int,
        komentar: String?
    ) {
        AppLogger.d("fname: $fname")
        AppLogger.d("resultCode: $resultCode")
        AppLogger.d("position: $position")

        if (shouldReopenBottomSheet) {
            shouldReopenBottomSheet = false

            bottomNavInspect.visibility = View.VISIBLE

            val currentPage = formAncakViewModel.currentPage.value ?: 1
            val currentData =
                formAncakViewModel.getPageData(currentPage) ?: FormAncakViewModel.PageData()

            formAncakViewModel.savePageData(currentPage, currentData.copy(photo = fname))

            Handler(Looper.getMainLooper()).postDelayed({
                showViewPhotoBottomSheet(fname)
            }, 100)
        }
    }

}