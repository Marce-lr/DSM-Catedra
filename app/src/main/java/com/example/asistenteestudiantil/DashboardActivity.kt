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
import com.example.asistenteestudiantil.databinding.ActivityDashboardBinding
import com.example.asistenteestudiantil.models.AcademicActivity
import com.example.asistenteestudiantil.models.Course
import com.example.asistenteestudiantil.models.Schedule
import com.example.asistenteestudiantil.utils.FirebaseDb
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

	private lateinit var binding: ActivityDashboardBinding
	private val schedules = mutableListOf<Schedule>()
	private val activities = mutableListOf<AcademicActivity>()
	private val courses = mutableListOf<Course>()
	
	// Adapters
	private lateinit var clasesAdapter: ClasesHoyAdapter
	private lateinit var actividadesAdapter: ActividadesPendientesAdapter
	private lateinit var calificacionesAdapter: CalificacionesAdapter
	
	// Referencias a listeners
	private var schedulesListener: ValueEventListener? = null
	private var activitiesListener: ValueEventListener? = null
	private var coursesListener: ValueEventListener? = null
	private var schedulesRef: com.google.firebase.database.DatabaseReference? = null
	private var activitiesRef: com.google.firebase.database.DatabaseReference? = null
	private var coursesRef: com.google.firebase.database.DatabaseReference? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityDashboardBinding.inflate(layoutInflater)
		setContentView(binding.root)

		// Configurar fecha actual
		val fechaActual = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES")).format(Date())
		binding.txtFecha.text = fechaActual.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

		// Inicializar adapters
		clasesAdapter = ClasesHoyAdapter()
		binding.recyclerClasesHoy.apply {
			layoutManager = LinearLayoutManager(this@DashboardActivity)
			adapter = clasesAdapter
		}

		actividadesAdapter = ActividadesPendientesAdapter { activity ->
			// Navegar a ActividadesActivity
			startActivity(Intent(this, ActividadesActivity::class.java))
		}
		binding.recyclerActividadesPendientes.apply {
			layoutManager = LinearLayoutManager(this@DashboardActivity)
			adapter = actividadesAdapter
		}

		calificacionesAdapter = CalificacionesAdapter()
		binding.recyclerCalificaciones.apply {
			layoutManager = LinearLayoutManager(this@DashboardActivity)
			adapter = calificacionesAdapter
		}

		setupBottomNavigation()
		subscribeToData()
	}

	private fun subscribeToData() {
		// Suscribirse a cursos
		coursesRef = FirebaseDb.coursesRef()
		coursesListener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (isDestroyed || isFinishing) return
				try {
					courses.clear()
					courses.addAll(FirebaseDb.mapCourses(snapshot))
					updateDashboard()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			override fun onCancelled(error: DatabaseError) {}
		}
		coursesRef?.addValueEventListener(coursesListener!!)

		// Suscribirse a schedules
		schedulesRef = FirebaseDb.schedulesRef()
		schedulesListener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (isDestroyed || isFinishing) return
				try {
					schedules.clear()
					schedules.addAll(FirebaseDb.mapSchedules(snapshot))
					updateDashboard()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			override fun onCancelled(error: DatabaseError) {}
		}
		schedulesRef?.addValueEventListener(schedulesListener!!)

		// Suscribirse a actividades
		activitiesRef = FirebaseDb.activitiesByCourseRef()
		activitiesListener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				if (isDestroyed || isFinishing) return
				try {
					activities.clear()
					activities.addAll(FirebaseDb.mapActivities(snapshot))
					updateDashboard()
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			override fun onCancelled(error: DatabaseError) {}
		}
		activitiesRef?.addValueEventListener(activitiesListener!!)
	}

	private fun updateDashboard() {
		if (isDestroyed || isFinishing) return

		// Clases del día
		val diaActual = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // 1=Domingo, 2=Lunes, ..., 7=Sábado
		val diaSemanaAndroid = diaActual // Android Calendar usa 1=Sunday, necesitamos convertir
		// Convertir: Android Calendar (1=Dom) -> Nuestro sistema (1=Lun)
		val diaNuestro = if (diaSemanaAndroid == Calendar.SUNDAY) 7 else diaSemanaAndroid - 1
		
		val clasesHoy = schedules.filter { it.dayOfWeek == diaNuestro }
			.sortedBy { it.startTime }
		
		if (clasesHoy.isEmpty()) {
			binding.txtSinClases.visibility = View.VISIBLE
			binding.recyclerClasesHoy.visibility = View.GONE
		} else {
			binding.txtSinClases.visibility = View.GONE
			binding.recyclerClasesHoy.visibility = View.VISIBLE
			clasesAdapter.submit(clasesHoy)
		}

		// Actividades pendientes (máximo 5, ordenadas por fecha de vencimiento)
		val actividadesPendientes = activities
			.filter { it.status != com.example.asistenteestudiantil.models.ActivityStatus.COMPLETADA }
			.sortedBy { it.dueDateMillis }
			.take(5)
		
		if (actividadesPendientes.isEmpty()) {
			binding.txtSinActividades.visibility = View.VISIBLE
			binding.recyclerActividadesPendientes.visibility = View.GONE
		} else {
			binding.txtSinActividades.visibility = View.GONE
			binding.recyclerActividadesPendientes.visibility = View.VISIBLE
			actividadesAdapter.submit(actividadesPendientes, courses)
		}

		// Últimas calificaciones (máximo 5, ordenadas por fecha de actualización descendente)
		val calificaciones = activities
			.filter { it.scoreObtained != null && it.status == com.example.asistenteestudiantil.models.ActivityStatus.COMPLETADA }
			.sortedByDescending { it.updatedAt }
			.take(5)
		
		if (calificaciones.isEmpty()) {
			binding.txtSinCalificaciones.visibility = View.VISIBLE
			binding.recyclerCalificaciones.visibility = View.GONE
		} else {
			binding.txtSinCalificaciones.visibility = View.GONE
			binding.recyclerCalificaciones.visibility = View.VISIBLE
			calificacionesAdapter.submit(calificaciones, courses)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		// Remover listeners
		schedulesListener?.let { schedulesRef?.removeEventListener(it) }
		activitiesListener?.let { activitiesRef?.removeEventListener(it) }
		coursesListener?.let { coursesRef?.removeEventListener(it) }
		schedulesListener = null
		activitiesListener = null
		coursesListener = null
		schedulesRef = null
		activitiesRef = null
		coursesRef = null
	}

	private fun setupBottomNavigation() {
		findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
			selectedItemId = R.id.nav_dashboard
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_dashboard -> true
					R.id.nav_apuntes -> { startActivity(Intent(this@DashboardActivity, ApuntesActivity::class.java)); finish(); true }
					R.id.nav_actividades -> { startActivity(Intent(this@DashboardActivity, ActividadesActivity::class.java)); finish(); true }
					R.id.nav_horarios -> { startActivity(Intent(this@DashboardActivity, HorariosActivity::class.java)); finish(); true }
					R.id.nav_calendario -> { startActivity(Intent(this@DashboardActivity, CalendarioActivity::class.java)); finish(); true }
					R.id.nav_perfil -> { startActivity(Intent(this@DashboardActivity, PerfilActivity::class.java)); finish(); true }
					else -> false
				}
			}
		}
	}
}

// Adapter para clases del día
private class ClasesHoyAdapter : RecyclerView.Adapter<ClaseHoyViewHolder>() {
	private var items: List<Schedule> = emptyList()

	fun submit(list: List<Schedule>) {
		items = list
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseHoyViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_clase_hoy, parent, false)
		return ClaseHoyViewHolder(view)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: ClaseHoyViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class ClaseHoyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	private val txtNombreClase: TextView = itemView.findViewById(R.id.txtNombreClase)
	private val txtHorario: TextView = itemView.findViewById(R.id.txtHorario)
	private val txtProfesorUbicacion: TextView = itemView.findViewById(R.id.txtProfesorUbicacion)

	fun bind(schedule: Schedule) {
		txtNombreClase.text = schedule.courseName
		txtHorario.text = "${schedule.startTime} - ${schedule.endTime}"
		txtProfesorUbicacion.text = "${schedule.professor} • ${schedule.room}"
	}
}

// Adapter para actividades pendientes
private class ActividadesPendientesAdapter(
	private val onClick: (AcademicActivity) -> Unit
) : RecyclerView.Adapter<ActividadPendienteViewHolder>() {
	private var items: List<AcademicActivity> = emptyList()
	private var courses: List<Course> = emptyList()

	fun submit(list: List<AcademicActivity>, coursesList: List<Course>) {
		items = list
		courses = coursesList
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActividadPendienteViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_actividad_pendiente, parent, false)
		return ActividadPendienteViewHolder(view, onClick, courses)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: ActividadPendienteViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class ActividadPendienteViewHolder(
	itemView: View,
	private val onClick: (AcademicActivity) -> Unit,
	private val courses: List<Course>
) : RecyclerView.ViewHolder(itemView) {
	private val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
	private val txtMateria: TextView = itemView.findViewById(R.id.txtMateria)
	private val txtFechaVencimiento: TextView = itemView.findViewById(R.id.txtFechaVencimiento)
	private val chipPrioridad = itemView.findViewById<com.google.android.material.chip.Chip>(R.id.chipPrioridad)

	fun bind(activity: AcademicActivity) {
		txtTitulo.text = activity.title
		
		// Obtener nombre de la materia
		val course = courses.find { it.id == activity.courseId }
		val materiaNombre = course?.name ?: "Sin materia"
		txtMateria.text = materiaNombre

		// Formatear fecha de vencimiento
		if (activity.dueDateMillis > 0) {
			val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(activity.dueDateMillis))
			txtFechaVencimiento.text = "Vence: $fecha"
		} else {
			txtFechaVencimiento.text = "Sin fecha de vencimiento"
		}

		// Configurar chip de prioridad
		chipPrioridad.text = activity.priority.name
		chipPrioridad.setChipBackgroundColorResource(
			when (activity.priority) {
				com.example.asistenteestudiantil.models.ActivityPriority.ALTA -> android.R.color.holo_red_light
				com.example.asistenteestudiantil.models.ActivityPriority.MEDIA -> android.R.color.holo_orange_light
				com.example.asistenteestudiantil.models.ActivityPriority.BAJA -> android.R.color.holo_green_light
			}
		)

		itemView.setOnClickListener { onClick(activity) }
	}
}

// Adapter para calificaciones
private class CalificacionesAdapter : RecyclerView.Adapter<CalificacionViewHolder>() {
	private var items: List<AcademicActivity> = emptyList()
	private var courses: List<Course> = emptyList()

	fun submit(list: List<AcademicActivity>, coursesList: List<Course>) {
		items = list
		courses = coursesList
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalificacionViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calificacion, parent, false)
		return CalificacionViewHolder(view, courses)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: CalificacionViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class CalificacionViewHolder(
	itemView: View,
	private val courses: List<Course>
) : RecyclerView.ViewHolder(itemView) {
	private val txtTitulo: TextView = itemView.findViewById(R.id.txtTitulo)
	private val txtMateria: TextView = itemView.findViewById(R.id.txtMateria)
	private val txtCalificacion: TextView = itemView.findViewById(R.id.txtCalificacion)

	fun bind(activity: AcademicActivity) {
		txtTitulo.text = activity.title
		
		// Obtener nombre de la materia
		val course = courses.find { it.id == activity.courseId }
		val materiaNombre = course?.name ?: "Sin materia"
		txtMateria.text = materiaNombre

		// Mostrar calificación
		activity.scoreObtained?.let { score ->
			txtCalificacion.text = String.format("%.1f", score)
		} ?: run {
			txtCalificacion.text = "N/A"
		}
	}
}

