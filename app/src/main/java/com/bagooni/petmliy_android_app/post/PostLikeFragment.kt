package com.bagooni.petmliy_android_app.post

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bagooni.petmliy_android_app.LoadingDialog
import com.bagooni.petmliy_android_app.MainActivity
import com.bagooni.petmliy_android_app.R
import com.bagooni.petmliy_android_app.databinding.FragmentPostLikeBinding
import com.bagooni.petmliy_android_app.post.Retrofit.Like
import com.bagooni.petmliy_android_app.post.Retrofit.LikeRetrofitService
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PostLikeFragment : Fragment() {
    var client: OkHttpClient? =
        httpLoggingInterceptor()?.let { OkHttpClient.Builder().addInterceptor(it).build() }

    private var _binding: FragmentPostLikeBinding?= null
    private val binding get() = _binding!!
    private var personEmailInput : String = ""
    private var userImgUri : String = ""

    lateinit var retrofitService: RetrofitService
    lateinit var likeRetrofitService: LikeRetrofitService
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostLikeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swipeRefreshLayout = view.findViewById(R.id.swipeLayout)
        swipeRefreshLayout.setOnRefreshListener {
            getLikePost()
            swipeRefreshLayout.isRefreshing = false
        }
        checkSign()
        val likeListView = binding.likeList
        var gson = GsonBuilder().setLenient().create()
        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-54-180-166-236.ap-northeast-2.compute.amazonaws.com:8080/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        retrofitService = retrofit.create(RetrofitService::class.java)
        likeRetrofitService = retrofit.create(LikeRetrofitService::class.java)
        getLikePost()
        binding.closeButton.setOnClickListener {
            findNavController().navigate(R.id.action_postLikeFragment_to_postFragment)
        }
    }

    class LikeRecyclerViewAdapter(
        val postList: ArrayList<Post>,
        val inflater: LayoutInflater,
        val glide: RequestManager,
        val postLikeFragment: PostLikeFragment,
        val activity: MainActivity
    ): RecyclerView.Adapter<LikeRecyclerViewAdapter.ViewHolder>(){

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
            val userImg : ImageView
            val userName : TextView
            val postUserName : TextView
            val postImg : ImageView
            val postContent : TextView
            val favoriteBtn : ImageButton
            val favoriteColorBtn : ImageButton
            val postLayer : ImageView
            val postHeart : ImageView
            val commentBtn : ImageButton
            val countLike : TextView
            val tagText : TextView
            val deleteBtn : ImageView

            init {
                userImg = itemView.findViewById(R.id.userImg)
                userName = itemView.findViewById(R.id.userEmail)
                postUserName = itemView.findViewById(R.id.postUserName)
                postImg = itemView.findViewById(R.id.postImg)
                postContent = itemView.findViewById(R.id.postContent)
                favoriteBtn = itemView.findViewById(R.id.favoriteBtn) //좋아요 버튼
                favoriteColorBtn = itemView.findViewById(R.id.favoriteColorBtn) //좋아요 색 버튼
                postLayer = itemView.findViewById(R.id.postLayer)
                postHeart = itemView.findViewById(R.id.postHeart)
                commentBtn = itemView.findViewById(R.id.commentBtn)
                countLike = itemView.findViewById(R.id.likeCount)
                tagText = itemView.findViewById(R.id.tagText)
                deleteBtn = itemView.findViewById(R.id.deleteButton)

                favoriteBtn.setOnClickListener {
                    Thread {
                        postLikeFragment.postLike(postList[adapterPosition].postId)
                        activity.runOnUiThread {
                            favoriteColorBtn.visibility = View.VISIBLE
                            postLayer.visibility = View.VISIBLE
                            postHeart.visibility = View.VISIBLE
                        }
                        Thread.sleep(1000)
                        activity.runOnUiThread {
                            postLayer.visibility = View.INVISIBLE
                            postHeart.visibility = View.INVISIBLE
                            postLikeFragment.getCountLike(postList[adapterPosition].postId, countLike)
                        }
                    }.start()
                }
                favoriteColorBtn.setOnClickListener {
                    Thread{
                        activity.runOnUiThread {
                            postLikeFragment.deleteData(postList[adapterPosition].postId)
                            favoriteColorBtn.visibility = View.INVISIBLE
                        }
                        Thread.sleep(1000)
                        activity.runOnUiThread {
                            postLikeFragment.getCountLike(postList[adapterPosition].postId, countLike)
                            postLikeFragment.getLikePost()
                        }
                    }.start()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.post_recyclerview_item,parent,false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = postList[position]

            post.userImg.let{
                glide.load(it).centerCrop().circleCrop().into(holder.userImg)
            }

            if (!post.postImg.isNullOrEmpty() ){
                val byte = Base64.decode(post.postImg, Base64.DEFAULT)
                val img: Bitmap = BitmapFactory.decodeByteArray(byte, 0, byte.size)
                holder.postImg.setImageBitmap(img)
            }
            holder.postUserName.text = post.email.split("@")[0]
            holder.userName.text = post.email.split("@")[0]
            holder.postContent.text = post.postContent
            ("#" + post.tags.replace(", ", " #")).also { holder.tagText.text = it }

            if (post.email == postLikeFragment.personEmailInput) {
                holder.deleteBtn.visibility = View.VISIBLE
            }

            postLikeFragment.likeRetrofitService.aboutLike(postLikeFragment.personEmailInput,post.postId)
                .enqueue(object : Callback<Int>{
                    override fun onResponse(call: Call<Int>, response: Response<Int>) {
                        Log.d("getPostLike",response.body().toString())
                        if (response.body() == 1){
                            Thread{
                                activity.runOnUiThread {
                                    holder.favoriteColorBtn.visibility = View.VISIBLE
                                }
                            }.start()
                        }else{
                            Thread{
                                activity.runOnUiThread {
                                    holder.favoriteColorBtn.visibility = View.INVISIBLE
                                }
                            }.start()
                        }
                    }
                    override fun onFailure(call: Call<Int>, t: Throwable) {
                    }
                })

            holder.deleteBtn.setOnClickListener {
                val builder = AlertDialog.Builder(activity as MainActivity)
                builder.setTitle("게시글을 삭제하시겠습니까?")
                    .setMessage("업로드한 게시글이 삭제됩니다.")
                    .setPositiveButton("확인", DialogInterface.OnClickListener{ dialog, id ->
                        Thread {
                            activity.runOnUiThread {
                                postLikeFragment.deletePost(post.postId)
                                Thread.sleep(1000)
                                Toast.makeText(
                                    activity as MainActivity,
                                    "게시글이 삭제되었습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                postLikeFragment.getLikePost()
                            }
                        }.start()
                    })
                    .setNegativeButton("취소", DialogInterface.OnClickListener{ dialog, id ->
                    })
                builder.show()
            }

            holder.commentBtn.setOnClickListener { postLikeFragment.postToComment(post.postId) }
            postLikeFragment.getCountLike(post.postId, holder.countLike)
            holder.commentBtn.setOnClickListener { postLikeFragment.postToComment(post.postId) }
        }

        override fun getItemCount(): Int {
            return postList.size
        }
    }

    private fun getLikePost(){
        val loading = LoadingDialog(activity as MainActivity)
        loading.show()
        retrofitService.getLikePost(personEmailInput).enqueue(object : Callback<ArrayList<Post>>{
            override fun onResponse(call: Call<ArrayList<Post>>, response: Response<ArrayList<Post>>) {
                val postList = response.body()
                val likeRecyclerView = view?.findViewById<RecyclerView>(R.id.likeList)
                likeRecyclerView?.adapter = postList?.let{
                    LikeRecyclerViewAdapter(
                        it,
                        LayoutInflater.from(activity),
                        Glide.with(activity!!),
                        this@PostLikeFragment,
                        activity as (MainActivity)
                    )
                }
                loading.dismiss()
            }

            override fun onFailure(call: Call<ArrayList<Post>>, t: Throwable) {
                Log.d("log",t.message.toString())
            }

        })
    }

    private fun getCountLike(postId: Long, textView: TextView) {
        likeRetrofitService.countLike(postId).enqueue(object : Callback<Int>{
            override fun onResponse(call: Call<Int>, response: Response<Int>) {
                textView.text = "좋아요 "+response.body().toString()+"개"
            }
            override fun onFailure(call: Call<Int>, t: Throwable) {
            }
        })
    }

    //좋아요 구현
    private fun postLike(postId: Long){
        likeRetrofitService.postLike(personEmailInput,postId,userImgUri).enqueue(object : Callback<Like>{
            override fun onResponse(call: Call<Like>, response: Response<Like>) {
            }
            override fun onFailure(call: Call<Like>, t: Throwable) {
            }

        })
    }

    //좋아요 취소
    private fun deleteData(postId: Long){
        likeRetrofitService.deleteLike(personEmailInput,postId).enqueue(object : Callback<Void>{
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("log",t.message.toString())
            }
        })
    }

    private fun deletePost(postId: Long) {
        retrofitService.deletePost(personEmailInput, postId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("log delete", t.message.toString())
            }
        })
    }

    private fun checkSign(){
        val acct = GoogleSignIn.getLastSignedInAccount(activity as MainActivity)
        if (acct != null) {
            val personEmail = acct.email
            val userImg = acct.photoUrl
            personEmailInput = personEmail.toString()
            userImgUri = userImg.toString()
        }
    }

    fun postToComment(postId : Long){
        var action = PostLikeFragmentDirections.actionPostLikeFragmentToCommentFragment(postId)
        findNavController().navigate(action)
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

