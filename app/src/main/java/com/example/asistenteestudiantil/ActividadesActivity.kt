package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asistenteestudiantil.databinding.ActivityActividadesBinding
import com.example.asistenteestudiantil.databinding.DialogActividadBinding
import com.example.asistenteestudiantil.models.*
import com.example.asistenteestudiantil.utils.FirebaseDb
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ActividadesActivity : AppCompatActivity() {

	private lateinit var binding: ActivityActividadesBinding
	private lateinit var adapter: ActividadAdapter
	private var courseId: String? = null
	private val courses = mutableListOf<Course>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityActividadesBinding.inflate(layoutInflater)
		setContentView(binding.root)

		courseId = intent.getStringExtra("courseId")

		adapter = ActividadAdapter(
			onEdit = { showUpsertDialog(it) },
			onDelete = { FirebaseDb.deleteActivity(it) }
		)
		binding.recycler.apply {
			layoutManager = LinearLayoutManager(this@ActividadesActivity)
			adapter = this@ActividadesActivity.adapter
		}

		binding.fabAdd.setOnClickListener { showUpsertDialog(null) }
		binding.filterGroup.setOnCheckedStateChangeListener { _: com.google.android.material.chip.ChipGroup, _: MutableList<Int> ->
			applyFilters()
		}

		subscribeToActivities()

		// Bottom navigation
		findViewById<BottomNavigationView>(R.id.bottomNav).apply {
			selectedItemId = R.id.nav_actividades
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_dashboard -> { startActivity(Intent(this@ActividadesActivity, DashboardActivity::class.java)); finish(); true }
					R.id.nav_actividades -> true
					R.id.nav_apuntes -> { startActivity(Intent(this@ActividadesActivity, ApuntesActivity::class.java)); finish(); true }
					R.id.nav_horarios -> { startActivity(Intent(this@ActividadesActivity, HorariosActivity::class.java)); finish(); true }
					R.id.nav_calendario -> { startActivity(Intent(this@ActividadesActivity, CalendarioActivity::class.java)); finish(); true }
					R.id.nav_perfil -> {
						startActivity(Intent(this@ActividadesActivity, PerfilActivity::class.java))
						finish()
						true
					}
					else -> false
				}
			}
		}
	}

	private fun subscribeToActivities() {
		// Cargar lista de materias para el spinner y mostrar nombres
		FirebaseDb.coursesRef().addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				courses.clear()
				courses.addAll(FirebaseDb.mapCourses(snapshot))
				adapter.updateCourses(courses)
			}
			override fun onCancelled(error: DatabaseError) {}
		})
		
		// Suscribirse a actividades
		if (courseId != null && courseId!!.isNotEmpty()) {
			// Suscribirse a actividades del Course específico
			FirebaseDb.courseActivitiesRef(courseId!!).addValueEventListener(object : ValueEventListener {
				override fun onDataChange(snapshot: DataSnapshot) {
					val list = FirebaseDb.mapActivities(snapshot)
					adapter.submit(list)
					applyFilters()
				}
				override fun onCancelled(error: DatabaseError) {}
			})
		} else {
			// Suscribirse a todas las actividades por Course (vista global)
			FirebaseDb.activitiesByCourseRef().addValueEventListener(object : ValueEventListener {
				override fun onDataChange(snapshot: DataSnapshot) {
					val list = FirebaseDb.mapActivities(snapshot)
					adapter.submit(list)
					applyFilters()
				}
				override fun onCancelled(error: DatabaseError) {}
			})
		}
	}

	private fun applyFilters() {
		val base = adapter.items
		val byStatus = when (binding.chipStatus.isChecked) {
			true -> base.sortedBy { it.status.name }
			false -> base
		}
		val byPriority = when (binding.chipPriority.isChecked) {
			true -> byStatus.sortedByDescending { it.priority }
			false -> byStatus
		}
		val byDate = when (binding.chipDate.isChecked) {
			true -> byPriority.sortedBy { it.dueDateMillis }
			false -> byPriority
		}
		adapter.submit(byDate)
	}

	private fun showUpsertDialog(existing: AcademicActivity?) {
		if (courses.isEmpty()) {
			com.google.android.material.snackbar.Snackbar.make(binding.root, "Primero debes crear al menos una materia en Horarios", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
			return
		}
		
		val dialogBinding = DialogActividadBinding.inflate(LayoutInflater.from(this))
		val view: View = dialogBinding.root

		dialogBinding.inputTitle.setText(existing?.title ?: "")
		dialogBinding.inputDescription.setText(existing?.description ?: "")
		dialogBinding.inputWeight.setText((existing?.weightPercent ?: 0.0).toString())
		dialogBinding.inputScore.setText(existing?.scoreObtained?.toString() ?: "")

		// Configurar spinner de materias
		val materiasAdapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			courses.map { "${it.name} (${it.code})" }
		)
		dialogBinding.spinnerMateria.adapter = materiasAdapter
		
		// Seleccionar materia existente si hay
		existing?.let { act ->
			val materiaIndex = courses.indexOfFirst { it.id == act.courseId }
			if (materiaIndex >= 0) {
				dialogBinding.spinnerMateria.setSelection(materiaIndex)
			}
		} ?: run {
			// Si es nueva y hay courseId predefinido, seleccionarlo
			courseId?.let { cId ->
				val materiaIndex = courses.indexOfFirst { it.id == cId }
				if (materiaIndex >= 0) {
					dialogBinding.spinnerMateria.setSelection(materiaIndex)
				}
			}
		}

		dialogBinding.spinnerType.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityType.values().map { it.name }
		)
		existing?.let {
			val typeIndex = ActivityType.values().indexOf(it.type)
			if (typeIndex >= 0) dialogBinding.spinnerType.setSelection(typeIndex)
		}

		dialogBinding.spinnerPriority.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityPriority.values().map { it.name }
		)
		existing?.let {
			val priorityIndex = ActivityPriority.values().indexOf(it.priority)
			if (priorityIndex >= 0) dialogBinding.spinnerPriority.setSelection(priorityIndex)
		}

		dialogBinding.spinnerStatus.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityStatus.values().map { it.name }
		)
		existing?.let {
			val statusIndex = ActivityStatus.values().indexOf(it.status)
			if (statusIndex >= 0) dialogBinding.spinnerStatus.setSelection(statusIndex)
		}

		MaterialAlertDialogBuilder(this)
			.setTitle(if (existing == null) getString(R.string.actividad_nueva) else getString(R.string.actividad_editar))
			.setView(view)
			.setNegativeButton(R.string.cancelar, null)
			.setPositiveButton(R.string.guardar) { _, _ ->
				// Obtener la materia seleccionada
				val selectedMateriaIndex = dialogBinding.spinnerMateria.selectedItemPosition
				if (selectedMateriaIndex < 0 || selectedMateriaIndex >= courses.size) {
					com.google.android.material.snackbar.Snackbar.make(binding.root, "Por favor selecciona una materia", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
					return@setPositiveButton
				}
				val selectedCourse = courses[selectedMateriaIndex]
				
				val now = System.currentTimeMillis()
				val act = AcademicActivity(
					id = existing?.id ?: "",
					subjectId = existing?.subjectId ?: "", // Mantener para compatibilidad
					courseId = selectedCourse.id,
					title = dialogBinding.inputTitle.text.toString().trim(),
					description = dialogBinding.inputDescription.text.toString().trim(),
					type = ActivityType.valueOf(dialogBinding.spinnerType.selectedItem as String),
					priority = ActivityPriority.valueOf(dialogBinding.spinnerPriority.selectedItem as String),
					status = ActivityStatus.valueOf(dialogBinding.spinnerStatus.selectedItem as String),
					dueDateMillis = existing?.dueDateMillis ?: now,
					weightPercent = dialogBinding.inputWeight.text.toString().toDoubleOrNull() ?: 0.0,
					scoreObtained = dialogBinding.inputScore.text.toString().toDoubleOrNull(),
					createdAt = existing?.createdAt ?: now,
					updatedAt = now
				)
				FirebaseDb.createOrUpdateActivity(act)
			}
			.show()
	}
}

private class ActividadAdapter(
	private val onEdit: (AcademicActivity) -> Unit,
	private val onDelete: (AcademicActivity) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ActividadViewHolder>() {

	var items: List<AcademicActivity> = emptyList(); private set
	private var courses: List<Course> = emptyList()

	fun submit(list: List<AcademicActivity>) {
		items = list
		notifyDataSetChanged()
	}
	
	fun updateCourses(coursesList: List<Course>) {
		courses = coursesList
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ActividadViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		val v = inflater.inflate(R.layout.item_actividad, parent, false)
		return ActividadViewHolder(v, onEdit, onDelete)
	}

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
		holder.bind(items[position], courses)
	}
}

private class ActividadViewHolder(
	itemView: View,
	private val onEdit: (AcademicActivity) -> Unit,
	private val onDelete: (AcademicActivity) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

	private val title: android.widget.TextView = itemView.findViewById(R.id.txtTitle)
	private val subtitle: android.widget.TextView = itemView.findViewById(R.id.txtSubtitle)
	private val btnEdit: View = itemView.findViewById(R.id.btnEdit)
	private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

	fun bind(item: AcademicActivity, courses: List<Course>) {
		title.text = item.title
		
		// Obtener nombre de la materia si existe
		val materiaNombre = if (item.courseId.isNotEmpty()) {
			courses.find { it.id == item.courseId }?.name ?: "Sin materia"
		} else {
			"Sin materia"
		}
		
		val subtitleText = if (itemView.context.resources.getIdentifier("item_actividad_subtitle", "string", itemView.context.packageName) != 0) {
			try {
				itemView.context.getString(
					R.string.item_actividad_subtitle,
					item.type.name,
					item.priority.name,
					item.status.name,
					item.weightPercent
				)
			} catch (e: Exception) {
				"${item.type.name} • ${item.priority.name} • ${item.status.name} • ${item.weightPercent}%"
			}
		} else {
			"${item.type.name} • ${item.priority.name} • ${item.status.name} • ${item.weightPercent}%"
		}
		
		subtitle.text = "$materiaNombre • $subtitleText"
		btnEdit.setOnClickListener { onEdit(item) }
		btnDelete.setOnClickListener { onDelete(item) }
	}
}


