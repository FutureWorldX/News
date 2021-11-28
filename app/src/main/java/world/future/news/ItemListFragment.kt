package world.future.news

import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject
import world.future.news.placeholder.PlaceholderContent
import world.future.news.databinding.FragmentItemListBinding
import world.future.news.databinding.ItemListContentBinding

class NewsArticle {
    val newsList: MutableList<String> = mutableListOf<String>()
    val article: String = ""
}

/**
 * A Fragment representing a list of Pings. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of items, which when touched,
 * lead to a {@link ItemDetailFragment} representing
 * item details. On larger screens, the Navigation controller presents the list of items and
 * item details side-by-side using two vertical panes.
 */

class ItemListFragment : Fragment() {


    //code to request permissions on startup
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }
    //val newsAPIKey = requireContext().packageManager.getApplicationInfo(requireContext().packageName, PackageManager.GET_META_DATA).metaData.getString("news_api_key")
    val newsAPIKey = getString(R.string.News_API_key)

    val newsEndpoint = "top-headlines" //options are top-headlines; everything;
    val newsCategory = "technology" //options are business; entertainment; general; health; science; sports; technology;
    val newsLanguage = "us"

    val serverAPIURL: String = "https://newsapi.org/v2/$newsEndpoint?country=$newsLanguage&apiKey=$newsAPIKey"
    val tagTitle = "News API"

    val newsList: MutableList<String> = mutableListOf<String>()

    //new code to getNews from the News API
    fun getNewsfromAPI(){

        val notification = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val mBuilder =  NotificationCompat.Builder(requireActivity())

        val queue: RequestQueue = Volley.newRequestQueue(context)
        val url = serverAPIURL
                //overriding the default JSONObjectRequest to add @Throws(AuthFailureError::class) override fun getHeaders():
        val request : JsonObjectRequest = object : JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->

                try {
                    val rootArray: JSONArray = response.getJSONArray("articles")
                    Log.e(tagTitle,"Number of entries retrieved: "+response.length().toString())
                    for(i in 0 until response.length()){
                        var dataObject: JSONObject = rootArray.get(i) as JSONObject
                        var item = rootArray[i].toString().replace("/","").replace("\\","/").replace("\r\n","")
                        newsList.add(item)
                        Log.d(tagTitle,item)
                        Toast.makeText(
                            context,
                            item,
                            Toast.LENGTH_SHORT
                        ).show()
                        notification.notify(i, mBuilder.build())
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "error while parsing the jsonObject/array",
                        Toast.LENGTH_LONG
                    ).show()
                }
                //callBack.gotTheNewsData(list)
            },
            { volleyError: VolleyError -> //error occurred in Volley
           Toast.makeText(context, "Error in response", Toast.LENGTH_SHORT).show()

       })
        {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["User-Agent"] = "Mozilla/5.0"
                return params
            }
        }
        queue.add(request)
    }

    //end of new code

    /**
     * Method to intercept global key events in the
     * item list fragment to trigger keyboard shortcuts
     * Currently provides a toast when Ctrl + Z and Ctrl + F
     * are triggered
     */
    private val unhandledKeyEventListenerCompat =
        ViewCompat.OnUnhandledKeyEventListenerCompat { v, event ->
            if (event.keyCode == KeyEvent.KEYCODE_Z && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Undo (Ctrl + Z) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else if (event.keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Find (Ctrl + F) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            false
        }

    private var _binding: FragmentItemListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {

        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // loading news at startup
        getNewsfromAPI()

        val recyclerView: RecyclerView = binding.itemList

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.item_detail_nav_container)

        /** Click Listener to trigger navigation based on if you have
         * a single pane layout or two pane layout
         */
        val onClickListener = View.OnClickListener { itemView ->
            //val item = itemView.tag as PlaceholderContent.PlaceholderItem
            for (i in 0 until newsList.size) {
            Log.e(tagTitle,newsList[i])

            val item = itemView.tag as PlaceholderContent.PlaceholderItem
            val bundle = Bundle()
            bundle.putString(
                newsList[i],
                newsList[i],
            )

            if (itemDetailFragmentContainer != null) {
                itemDetailFragmentContainer.findNavController()
                    .navigate(R.id.fragment_item_detail, bundle)

                //test code loads JSON as String
                Toast.makeText(
                    activity,
                    "newsAPIGetData method picked via id News API " + item.id,
                    Toast.LENGTH_LONG
                ).show()

            } else {
                itemView.findNavController().navigate(R.id.show_item_detail, bundle)
                //test code load as JSONObject
                Toast.makeText(
                    activity,
                    "getNews method without id load News API " + item.id,
                    Toast.LENGTH_LONG
                ).show()
            }
            }
        }

        /**
         * Context click listener to handle Right click events
         * from mice and trackpad input to provide a more native
         * experience on larger screen devices
         */
        val onContextClickListener = View.OnContextClickListener { v ->
            val item = v.tag as PlaceholderContent.PlaceholderItem
            Toast.makeText(
                v.context,
                "Context click of item " + item.id,
                Toast.LENGTH_LONG
            ).show()
            true
        }
        setupRecyclerView(recyclerView, onClickListener, onContextClickListener)
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        onClickListener: View.OnClickListener,
        onContextClickListener: View.OnContextClickListener
    ) {

        recyclerView.adapter = SimpleItemRecyclerViewAdapter(
            PlaceholderContent.ITEMS,
            onClickListener,
            onContextClickListener
        )
    }

    class SimpleItemRecyclerViewAdapter(
        private val values: List<PlaceholderContent.PlaceholderItem>,
        private val onClickListener: View.OnClickListener,
        private val onContextClickListener: View.OnContextClickListener
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

            val binding =
                ItemListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id
            holder.contentView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setOnContextClickListener(onContextClickListener)
                }

                setOnLongClickListener { v ->
                    // Setting the item id as the clip data so that the drop target is able to
                    // identify the id of the content
                    val clipItem = ClipData.Item(item.id)
                    val dragData = ClipData(
                        v.tag as? CharSequence,
                        arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                        clipItem
                    )

                    if (Build.VERSION.SDK_INT >= 24) {
                        v.startDragAndDrop(
                            dragData,
                            View.DragShadowBuilder(v),
                            null,
                            0
                        )
                    } else {
                        v.startDrag(
                            dragData,
                            View.DragShadowBuilder(v),
                            null,
                            0
                        )
                    }
                }
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(binding: ItemListContentBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val idView: TextView = binding.idText
            val contentView: TextView = binding.content
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}