package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.asistenteestudiantil.databinding.ActivityNoticiasBinding
import com.example.asistenteestudiantil.models.News
import com.example.asistenteestudiantil.utils.ApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoticiasActivity : AppCompatActivity() {

	private lateinit var binding: ActivityNoticiasBinding
	private lateinit var adapter: NoticiasAdapter
	private val newsList = mutableListOf<News>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityNoticiasBinding.inflate(layoutInflater)
		setContentView(binding.root)

		adapter = NoticiasAdapter(newsList)
		binding.recyclerNoticias.apply {
			layoutManager = LinearLayoutManager(this@NoticiasActivity)
			adapter = this@NoticiasActivity.adapter
		}

		setupBottomNavigation()
		loadNews()
	}

	private fun loadNews() {
		binding.progress.visibility = View.VISIBLE
		binding.txtEmpty.visibility = View.GONE

		CoroutineScope(Dispatchers.IO).launch {
			try {
				val response = ApiClient.apiService.getNews()
				withContext(Dispatchers.Main) {
					binding.progress.visibility = View.GONE
					if (response.isSuccessful && response.body() != null) {
						newsList.clear()
						newsList.addAll(response.body()!!)
						adapter.notifyDataSetChanged()
						
						if (newsList.isEmpty()) {
							binding.txtEmpty.visibility = View.VISIBLE
						} else {
							binding.txtEmpty.visibility = View.GONE
						}
					} else {
						showError("Error al cargar noticias: ${response.message()}")
						binding.txtEmpty.visibility = View.VISIBLE
						binding.txtEmpty.text = "Error al cargar noticias"
					}
				}
			} catch (e: Exception) {
				withContext(Dispatchers.Main) {
					binding.progress.visibility = View.GONE
					showError("Error de conexión: ${e.message}")
					binding.txtEmpty.visibility = View.VISIBLE
					binding.txtEmpty.text = "Error de conexión. Verifica tu internet"
				}
			}
		}
	}

	private fun showError(message: String) {
		Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
	}

	private fun setupBottomNavigation() {
		findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
			selectedItemId = R.id.nav_calendario // Seleccionar Calendario ya que Noticias está relacionada
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_dashboard -> { startActivity(Intent(this@NoticiasActivity, DashboardActivity::class.java)); finish(); true }
					R.id.nav_apuntes -> { startActivity(Intent(this@NoticiasActivity, ApuntesActivity::class.java)); finish(); true }
					R.id.nav_actividades -> { startActivity(Intent(this@NoticiasActivity, ActividadesActivity::class.java)); finish(); true }
					R.id.nav_horarios -> { startActivity(Intent(this@NoticiasActivity, HorariosActivity::class.java)); finish(); true }
					R.id.nav_calendario -> { startActivity(Intent(this@NoticiasActivity, CalendarioActivity::class.java)); finish(); true }
					R.id.nav_perfil -> { startActivity(Intent(this@NoticiasActivity, PerfilActivity::class.java)); finish(); true }
					else -> false
				}
			}
		}
	}
}

private class NoticiasAdapter(
	private val items: List<News>
) : RecyclerView.Adapter<NoticiaViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticiaViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_noticia, parent, false)
		return NoticiaViewHolder(view)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: NoticiaViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class NoticiaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
	private val txtContent: TextView = itemView.findViewById(R.id.txtContent)
	private val txtAuthor: TextView = itemView.findViewById(R.id.txtAuthor)
	private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
	private val chipCategory = itemView.findViewById<com.google.android.material.chip.Chip>(R.id.chipCategory)

	fun bind(news: News) {
		txtTitle.text = news.title
		txtContent.text = news.content
		txtAuthor.text = "Por: ${news.author}"
		txtDate.text = formatDate(news.publishedAt)
		chipCategory.text = news.category
	}

	private fun formatDate(dateString: String): String {
		return try {
			// Intentar formatear la fecha si viene en formato ISO o similar
			val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(dateString)
			date?.let {
				java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
			} ?: dateString
		} catch (e: Exception) {
			dateString
		}
	}
}

