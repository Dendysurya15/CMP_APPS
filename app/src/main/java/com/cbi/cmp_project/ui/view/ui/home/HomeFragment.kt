package com.cbi.cmp_project.ui.view.ui.home

import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cbi.cmp_project.R
import com.cbi.cmp_project.databinding.FragmentHomeBinding
import com.cbi.cmp_project.ui.view.FeaturePanenTBSActivity
import com.cbi.cmp_project.ui.view.generate_espb.GenerateEspbActivity
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.GZIPInputStream

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var featureAdapter: FeatureAdapter
    private lateinit var homeViewModel: HomeViewModel



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        observeViewModel()

        val downloadedFile = File(requireContext().getExternalFilesDir(null), "dataset_tph.txt")
//        decompressFile(downloadedFile)
        return root
    }


    private fun decompressFile(file: File) {
        try {
            // Read the entire content as a Base64-encoded string
            val base64String = file.readText()

            // Decode the Base64 string
            val compressedData = Base64.decode(base64String, Base64.DEFAULT)

            // Decompress using GZIP
            val gzipInputStream = GZIPInputStream(ByteArrayInputStream(compressedData))
            val decompressedData = gzipInputStream.readBytes()

            // Convert the decompressed bytes to a JSON string
            val jsonString = String(decompressedData)
            Log.d("DecompressedJSON", "Decompressed JSON: $jsonString")

            // Now you can log or process the JSON data
            val jsonObject = JSONObject(jsonString)
            Log.d("DecompressedJSON", "Available keys: ${jsonObject.keys().asSequence().toList()}")

        } catch (e: Exception) {
            Log.e("DecompressFile", "Error decompressing file: ${e.message}")
            e.printStackTrace()
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
                is FeatureCardEvent.NavigateToGenerateESPB -> {
                    event.context?.let {
                        val intent = Intent(it, GenerateEspbActivity::class.java)
                        // Pass the feature name to the intent
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
//            FeatureCard(
//                cardBackgroundColor = R.color.orange,
//                featureName = "Absensi",
//                featureNameBackgroundColor = R.color.yellowdarker,
//                iconResource = R.drawable.baseline_check_24,
//                functionName = "Lapor Absensi Anda",
//                functionDescription = "Harap Absensi Untuk Memulai Mengerjakan Pekerjaan Anda!",
//                displayType = DisplayType.ICON
//            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Panen TBS",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                count = null,
                functionName = "Tambah Lapor Panen",
                functionDescription = "Catat & Lapor Hasil\nPanen TPH anda disini!",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDefault,
                featureName = "Panen TBS",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionName = "Data Tersimpan",
                functionDescription = "Jumlah Data Panen Yang Sudah dibuat!",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Generate eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = R.drawable.cbi,
                functionName = "e-SPB PANEN",
                functionDescription = "Buat Laporan e-SPB & Transfer dengan NFC/QR Code",
                displayType = DisplayType.ICON
            ),
            FeatureCard(
                cardBackgroundColor = R.color.greenDarkerLight,
                featureName = "Generate eSPB",
                featureNameBackgroundColor = R.color.greenDarker,
                iconResource = null,
                count = "0",
                functionName = "e-SPB PANEN",
                functionDescription = "Buat Laporan e-SPB & Transfer dengan NFC/QR Code",
                displayType = DisplayType.COUNT
            ),
            FeatureCard(
                cardBackgroundColor = R.color.bluedarklight,
                featureName = "Inspeksi Panen",
                featureNameBackgroundColor = R.color.bluedark,
                iconResource = R.drawable.cbi,
                functionName = "Pemeriksaan Ancak",
                functionDescription = "Buat Catatan Sidak Path anda disini!",
                displayType = DisplayType.ICON
            ),


        )

        val gridLayoutManager = GridLayoutManager(context, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }

        binding.featuresRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = FeatureAdapter { featureCard ->
                // Pass the context to the ViewModel for navigation handling
                homeViewModel.onFeatureCardClicked(featureCard, requireContext())
            }.also {
                it.setFeatures(features)
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
    val functionName: String,
    val functionDescription: String,
    val displayType: DisplayType
)

enum class DisplayType {
    ICON,
    COUNT
}


class FeatureAdapter(private val onFeatureClicked: (FeatureCard) -> Unit)  : RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {

    private var features = listOf<FeatureCard>()

    fun setFeatures(newFeatures: List<FeatureCard>) {
        features = newFeatures
        notifyDataSetChanged()
    }

    class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.card_panen_tbs)
        val featureName: TextView = itemView.findViewById(R.id.feature_name)
        val featureNameBackground: FrameLayout = itemView.findViewById(R.id.bg_feature_name)
        val iconFeature: ImageView = itemView.findViewById(R.id.icon_feature)
        val countFeature: TextView = itemView.findViewById(R.id.count_feature_data)
        val functionName: TextView = itemView.findViewById(R.id.feature_function_name)
        val functionDescription: TextView = itemView.findViewById(R.id.feature_function_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_feature, parent, false)
        return FeatureViewHolder(view)
    }



    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        val feature = features[position]
        val context = holder.itemView.context

        // Set card background color
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, feature.cardBackgroundColor))

        // Set feature name and its background
        holder.featureName.text = feature.featureName
        (holder.featureNameBackground.background as GradientDrawable).setColor(
            ContextCompat.getColor(context, feature.featureNameBackgroundColor)
        )


        // Handle icon/count display
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

        // Set function name and description
        holder.functionName.text = feature.functionName
        holder.functionDescription.text = feature.functionDescription

        holder.cardView.setOnClickListener {
            onFeatureClicked(feature)
        }
    }

    override fun getItemCount() = features.size



}