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
import com.example.asistenteestudiantil.databinding.ActivityCalendarioBinding
import com.example.asistenteestudiantil.models.CalendarEvent
import com.example.asistenteestudiantil.utils.ApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalendarioActivity : AppCompatActivity() {

	private lateinit var binding: ActivityCalendarioBinding
	private lateinit var adapter: EventosAdapter
	private val eventosList = mutableListOf<CalendarEvent>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityCalendarioBinding.inflate(layoutInflater)
		setContentView(binding.root)

		adapter = EventosAdapter(eventosList)
		binding.recyclerEventos.apply {
			layoutManager = LinearLayoutManager(this@CalendarioActivity)
			adapter = this@CalendarioActivity.adapter
		}

		// Botón para acceder a Noticias
		binding.btnNoticias.setOnClickListener {
			startActivity(Intent(this, NoticiasActivity::class.java))
		}

		setupBottomNavigation()
		loadEventos()
	}

	private fun loadEventos() {
		binding.progress.visibility = View.VISIBLE
		binding.txtEmpty.visibility = View.GONE

		CoroutineScope(Dispatchers.IO).launch {
			try {
				val response = ApiClient.apiService.getCalendarEvents()
				withContext(Dispatchers.Main) {
					binding.progress.visibility = View.GONE
					if (response.isSuccessful && response.body() != null) {
						eventosList.clear()
						// Ordenar eventos: importantes primero, luego por fecha
						val sorted = response.body()!!.sortedWith(
							compareByDescending<CalendarEvent> { it.isImportant }
								.thenBy { it.startDate }
						)
						eventosList.addAll(sorted)
						adapter.notifyDataSetChanged()
						
						if (eventosList.isEmpty()) {
							binding.txtEmpty.visibility = View.VISIBLE
						} else {
							binding.txtEmpty.visibility = View.GONE
						}
					} else {
						showError("Error al cargar eventos: ${response.message()}")
						binding.txtEmpty.visibility = View.VISIBLE
						binding.txtEmpty.text = "Error al cargar eventos"
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
			selectedItemId = R.id.nav_calendario
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_dashboard -> { startActivity(Intent(this@CalendarioActivity, DashboardActivity::class.java)); finish(); true }
					R.id.nav_apuntes -> { startActivity(Intent(this@CalendarioActivity, ApuntesActivity::class.java)); finish(); true }
					R.id.nav_actividades -> { startActivity(Intent(this@CalendarioActivity, ActividadesActivity::class.java)); finish(); true }
					R.id.nav_horarios -> { startActivity(Intent(this@CalendarioActivity, HorariosActivity::class.java)); finish(); true }
					R.id.nav_calendario -> true
					R.id.nav_perfil -> { startActivity(Intent(this@CalendarioActivity, PerfilActivity::class.java)); finish(); true }
					else -> false
				}
			}
		}
	}
}

private class EventosAdapter(
	private val items: List<CalendarEvent>
) : RecyclerView.Adapter<EventoViewHolder>() {

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento_calendario, parent, false)
		return EventoViewHolder(view)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class EventoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	private val txtTitle: TextView = itemView.findViewById(R.id.txtTitle)
	private val txtDescription: TextView = itemView.findViewById(R.id.txtDescription)
	private val txtDate: TextView = itemView.findViewById(R.id.txtDate)
	private val txtLocation: TextView = itemView.findViewById(R.id.txtLocation)
	private val chipImportant = itemView.findViewById<com.google.android.material.chip.Chip>(R.id.chipImportant)
	private val chipType = itemView.findViewById<com.google.android.material.chip.Chip>(R.id.chipType)

	fun bind(evento: CalendarEvent) {
		txtTitle.text = evento.title
		txtDescription.text = evento.description
		txtDate.text = formatDate(evento.startDate, evento.endDate)
		
		if (evento.location != null && evento.location.isNotEmpty()) {
			txtLocation.text = evento.location
			txtLocation.visibility = View.VISIBLE
		} else {
			txtLocation.visibility = View.GONE
		}
		
		if (evento.isImportant) {
			chipImportant.visibility = View.VISIBLE
		} else {
			chipImportant.visibility = View.GONE
		}
		
		chipType.text = evento.type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
	}

	private fun formatDate(startDate: String, endDate: String?): String {
		return try {
			val start = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(startDate)
			val formattedStart = start?.let {
				java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
			} ?: startDate
			
			if (endDate != null && endDate.isNotEmpty()) {
				val end = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(endDate)
				val formattedEnd = end?.let {
					java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it)
				} ?: endDate
				"$formattedStart - $formattedEnd"
			} else {
				formattedStart
			}
		} catch (e: Exception) {
			startDate
		}
	}
}

