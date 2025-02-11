package com.cbi.cmp_project.ui.view.ui.home

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.databinding.FragmentHomeBinding

import com.cbi.cmp_project.ui.view.PanenTBS.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.PanenTBS.ListPanenTBSActivity
import com.cbi.cmp_project.ui.viewModel.PanenViewModel
import com.cbi.cmp_project.utils.AlertDialogUtility
import com.cbi.cmp_project.utils.AppLogger
import com.cbi.cmp_project.utils.AppUtils.stringXML
import com.cbi.cmp_project.utils.LoadingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cbi.cmp_project.ui.view.ScanQR
import es.dmoral.toasty.Toasty


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var featureAdapter: FeatureAdapter
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var panenViewModel: PanenViewModel
    private lateinit var loadingDialog: LoadingDialog

    private var countPanenTPH: Int = 0  // Global variable for count
    private var countPanenTPHApproval: Int = 0  // Global variable for count

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initViewModel()
        setupRecyclerView()
        observeViewModel()
        handleOnBackPressed()
        return root
    }

    private fun initViewModel() {
        val factory = PanenViewModel.PanenViewModelFactory(requireActivity().application)
        panenViewModel = ViewModelProvider(this, factory)[PanenViewModel::class.java]
    }

    private fun fetchDataEachCard() {

        if (this::featureAdapter.isInitialized) {  // Changed to positive condition
            lifecycleScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    featureAdapter.showLoadingForFeature("Rekap Hasil Panen")
                    delay(300)
                }
                try {
                    val countDeferred = async { panenViewModel.loadPanenCount() }
                    countPanenTPH = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount("Rekap Hasil Panen", countPanenTPH.toString())
                        featureAdapter.hideLoadingForFeature("Rekap Hasil Panen")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap Hasil Panen")
                    }
                }
                try {
                    val countDeferred = async { panenViewModel.loadPanenCountApproval() }
                    countPanenTPHApproval = countDeferred.await()
                    withContext(Dispatchers.Main) {
                        featureAdapter.updateCount("Rekap panen dan restan", countPanenTPHApproval.toString())
                        featureAdapter.hideLoadingForFeature("Rekap panen dan restan")
                    }
                } catch (e: Exception) {
                    AppLogger.e("Error fetching data: ${e.message}")
                    withContext(Dispatchers.Main) {
                        featureAdapter.hideLoadingForFeature("Rekap panen dan restan")
                    }
                }
            }
        } else {
            AppLogger.e("Feature adapter not initialized yet")
        }
    }


    private fun handleOnBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog(){
        AlertDialogUtility.withTwoActions(
            requireActivity(),
            requireContext().stringXML(R.string.al_yes),
            requireContext().stringXML(R.string.confirmation_dialog_title),
            requireContext().stringXML(R.string.al_confirm_out),
            "warning.json"
        ) {
            AppDatabase.closeDatabase()
            requireActivity().finishAffinity()
        }
    }

    private fun observeViewModel() {
        homeViewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is FeatureCardEvent.NavigateToPanenTBS -> {
                    event.context?.let {

                        val intent = Intent(it, FeaturePanenTBSActivity::class.java)
                        // Pass the feature name to the intent
                        event.featureName?.let { featureName ->
                            intent.putExtra("FEATURE_NAME", featureName)
                        }
                        startActivity(intent)
                    }
                }

                is FeatureCardEvent.NavigateToListPanenTBS -> {
                    event.context?.let {

                        val intent = Intent(it, ListPanenTBSActivity::class.java)
                        event.featureName?.let { featureName ->
                            intent.putExtra("FEATURE_NAME", featureName)
                        }
                        startActivity(intent)
                    }
                }

                is FeatureCardEvent.NavigateToScanPanen -> {
                    event.context?.let {
                        val intent = Intent(it, ScanQR::class.java)
                        // Pass the feature name to the intent
                        event.featureName?.let { featureName ->
                            Log.d("testing","masuk: ${event.featureName}")
                            intent.putExtra("FEATURE_NAME", featureName)
                        }
                        startActivity(intent)
                    }
                }

                is FeatureCardEvent.NavigateToBuatESPB -> {
                    event.context?.let {
                        val intent = Intent(it, ScanQR::class.java)
                        event.featureName?.let { featureName ->
                            Log.d("testing","masuk: ${event.featureName}")
                            intent.putExtra("FEATURE_NAME", featureName)
                        }
                        startActivity(intent)
                    }
                }
                is FeatureCardEvent.NavigateToRekapPanen -> {
                    Toasty.success(requireContext(), "Rekap Panen", Toasty.LENGTH_SHORT).show()
                    event.context?.let {
                        val intent = Intent(it, ListPanenTBSActivity::class.java)
                        event.featureName?.let { featureName ->
                            intent.putExtra("FEATURE_NAME", featureName)
                        }
                        startActivity(intent)
                    }
                }

            }
        }
    }

    private fun setupRecyclerView() {
        val features = listOf(
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Panen TBS",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                count = null,
                functionDescription = "Pencatatatan panen TBS di TPH oleh kerani panen",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Rekap Hasil Panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = countPanenTPH.toString(),
                functionDescription = "Rekapitulasi panen TBS dan transfer data ke suoervisi",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Scan Hasil Panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                count = null,
                functionDescription = "Transfer data dari kerani panen ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Rekap panen dan restan",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = countPanenTPHApproval.toString(),
                functionDescription = "Rekapitulsasi panen TBS dan restan dari kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Buat eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Transfer data dari driver ke supervisi untuk pembuatan eSPB",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi eSPB dan transfer data ke driver",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Inspeksi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "............",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap inspeksi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "............",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Absensi kehadiran karyawan panen oleh supervisi",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Rekap absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionDescription = "Rekapitulasi absensi karyawan dan transfer data ke kerani panen",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Scan absensi panen",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "Transfer data abseni dari supervisi ke kerani panen",
                displayType = DisplayType.ICON,
                subTitle = "Scan QR Code eSPB"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Weight bridge",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Transfer data eSPB dari driver"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Sinkronisasi data",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Sinkronisasi data manual"
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Asistensi estate",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionDescription = "",
                displayType = DisplayType.ICON,
                subTitle = "Panen TBS khusus asistensi dari estate lain"
            )
        )

        val gridLayoutManager = GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

        binding.featuresRecyclerView.apply {
            layoutManager = gridLayoutManager
            featureAdapter = FeatureAdapter { featureCard ->
                homeViewModel.onFeatureCardClicked(featureCard, requireContext())
            }

            // Set the adapter
            adapter = featureAdapter

            // Set features
            featureAdapter.setFeatures(features)

            post {
                fetchDataEachCard()
            }
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                    outRect.left = spacing
                    outRect.right = spacing
                    outRect.top = spacing
                    outRect.bottom = spacing
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class FeatureCard(
    val cardBackgroundColor: Int,
    val featureName: String,
    val featureNameBackgroundColor: Int,
    val iconResource: Int? = null,  // Make it nullable
    val count: String? = null,      // Add count parameter
    val functionDescription: String,
    val displayType: DisplayType,
    val subTitle: String? = ""
)

enum class DisplayType {
    ICON,
    COUNT
}


class FeatureAdapter(private val onFeatureClicked: (FeatureCard) -> Unit) : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    private var features = ArrayList<FeatureCard>()  // Changed to ArrayList for mutability

    fun setFeatures(newFeatures: List<FeatureCard>) {
        features = ArrayList(newFeatures)  // Simply create new ArrayList from input
        notifyDataSetChanged()
    }

    fun updateCount(featureName: String, newCount: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            val updatedFeature = features[position].copy(count = newCount)
            features[position] = updatedFeature  // Now this will work because ArrayList is mutable
            notifyItemChanged(position)
        }
    }

    fun showLoadingForFeature(featureName: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            notifyItemChanged(position, "show_loading")
        }
    }

    fun hideLoadingForFeature(featureName: String) {
        val position = features.indexOfFirst {
            it.featureName == featureName && it.displayType == DisplayType.COUNT
        }
        if (position != -1) {
            notifyItemChanged(position, "hide_loading")
        }
    }

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_panen_tbs)
        val featureName: TextView = itemView.findViewById(R.id.feature_name)
        val featureNameBackground: CardView = itemView.findViewById(R.id.card_panen_tbs_border)
        val iconFeature: ImageView = itemView.findViewById(R.id.icon_feature)
        val countFeature: TextView = itemView.findViewById(R.id.count_feature_data)
        val functionDescription: TextView = itemView.findViewById(R.id.feature_function_description)
        val loadingDotsContainer: LinearLayout = itemView.findViewById(R.id.countLoadingDotsContainer)
        val dot1: TextView = itemView.findViewById(R.id.countDot1)
        val dot2: TextView = itemView.findViewById(R.id.countDot2)
        val dot3: TextView = itemView.findViewById(R.id.countDot3)
        val dot4: TextView = itemView.findViewById(R.id.countDot4)

        fun showLoadingAnimation() {
            val dots = listOf(dot1, dot2, dot3, dot4)
            countFeature.visibility = View.INVISIBLE
            loadingDotsContainer.visibility = View.VISIBLE

            dots.forEachIndexed { index, dot ->
                val animation = ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f, 0f)
                animation.duration = 500
                animation.repeatCount = ObjectAnimator.INFINITE
                animation.repeatMode = ObjectAnimator.REVERSE
                animation.startDelay = (index * 100).toLong()
                animation.start()
            }
        }

        fun hideLoadingAnimation() {
            loadingDotsContainer.visibility = View.GONE
            countFeature.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_feature, parent, false)
        return FeatureViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        val context = holder.itemView.context

//        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, feature.cardBackgroundColor))
        holder.featureName.text = feature.featureName
        holder.featureNameBackground.setCardBackgroundColor(
            ContextCompat.getColor(context, feature.featureNameBackgroundColor)
        )
        when (feature.displayType) {
            DisplayType.ICON -> {
                holder.iconFeature.visibility = View.VISIBLE
                holder.countFeature.visibility = View.GONE
                feature.iconResource?.let { holder.iconFeature.setImageResource(it) }
            }
            DisplayType.COUNT -> {
                holder.iconFeature.visibility = View.GONE
                holder.countFeature.visibility = View.VISIBLE
                holder.countFeature.text = feature.count
            }
        }

        holder.functionDescription.text = feature.functionDescription
        holder.cardView.setOnClickListener {
            onFeatureClicked(feature)
        }
    }

    override fun onBindViewHolder(
        holder: FeatureViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (payloads[0]) {
                "show_loading" -> holder.showLoadingAnimation()
                "hide_loading" -> holder.hideLoadingAnimation()
                else -> super.onBindViewHolder(holder, position, payloads)
            }
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun getItemCount() = features.size
}