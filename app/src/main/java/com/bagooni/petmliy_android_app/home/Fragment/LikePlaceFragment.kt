package com.bagooni.petmliy_android_app.home.Fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bagooni.petmliy_android_app.R
import com.bagooni.petmliy_android_app.databinding.FragmentLikeplaceBinding
import com.bagooni.petmliy_android_app.home.Fragment.Api.LikePlaceApi
import com.bagooni.petmliy_android_app.home.Fragment.adapter.LikePlaceRecyclerAdapter
import com.bagooni.petmliy_android_app.map.model.Api.CustomMapApi
import com.bagooni.petmliy_android_app.map.model.Dto.LikePlaceDto
import com.bagooni.petmliy_android_app.walk.Fragment.Api.CustomWalkApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LikePlaceFragment : Fragment() {
    var client: OkHttpClient? =
        httpLoggingInterceptor()?.let { OkHttpClient.Builder().addInterceptor(it).build() }

    private var _binding: FragmentLikeplaceBinding?=null
    private val binding get() = _binding!!

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private var googleEmail: String? = null

    private val recyclerView: RecyclerView by lazy {
        binding.recyclerView
    }

    private val recyclerAdapter = LikePlaceRecyclerAdapter(deleteButton = { place ->
        Log.d("map",place.placeId.toString())
        place.placeId?.let { customAPi(it) }
    }, linkButton = { place ->
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(place.url)
        }
        startActivity(intent)
    }, shareButton = { place ->
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, place.url)
        }
        startActivity(Intent.createChooser(sharingIntent, "공유하기"))
    })

    private fun customAPi(placeId: Int) {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-54-180-166-236.ap-northeast-2.compute.amazonaws.com:8080")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(CustomMapApi::class.java)

        googleEmail?.let { email ->
            api.deletePlace(email, placeId)
        }?.enqueue(object : Callback<Void> {

            override fun onResponse(
                call: Call<Void>,
                response: Response<Void>
            ) {Log.d("map", "2")
                if (!response.isSuccessful) {
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("map Error", t.message.toString())
            }

        })
        Toast.makeText(context,"장소가 삭제되었습니다.",Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.bookMarkFragment)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            updateUI(account)
        } catch (e: ApiException) {
            Log.w("Google", "signInResult:failed code=" + e.statusCode)
            updateUI(null)
        }
    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            googleEmail = account.email
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLauncher()
        googleSet()
    }

    private fun initLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode != AppCompatActivity.RESULT_OK) {
                    return@registerForActivityResult
                }
                val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                handleSignInResult(task)
            }
    }

    private fun googleSet() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(activity).also{
            it.reverseLayout = true
            it.stackFromEnd = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLikeplaceBinding.inflate(inflater,container,false)
        binding.closeButton.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        return binding.root
    }

    private fun customAPi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-54-180-166-236.ap-northeast-2.compute.amazonaws.com:8080")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(LikePlaceApi::class.java)
        val allData = googleEmail?.let { email ->
            api.searchAllData(email)
        }

        allData?.enqueue(object : Callback<List<LikePlaceDto>> {
            override fun onResponse(
                call: Call<List<LikePlaceDto>>,
                response: Response<List<LikePlaceDto>>
            ) {
                response.body().let{ dto ->
                    recyclerAdapter.submitList(dto)
                }
            }

            override fun onFailure(call: Call<List<LikePlaceDto>>, t: Throwable) {
                Log.d("bookmark",t.message.toString())
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            updateUI(account)
        }
        customAPi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun httpLoggingInterceptor(): HttpLoggingInterceptor? {
        val interceptor = HttpLoggingInterceptor { message ->
            Log.e(
                "MyGitHubData :",
                message + ""
            )
        }
        return interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
    }
}