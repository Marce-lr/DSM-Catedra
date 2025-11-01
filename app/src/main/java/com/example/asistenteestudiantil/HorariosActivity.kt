package com.example.asistenteestudiantil

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.asistenteestudiantil.databinding.ActivityHorariosBinding
import com.example.asistenteestudiantil.databinding.DialogMateriaBinding
import com.example.asistenteestudiantil.models.Course
import com.example.asistenteestudiantil.models.Schedule
import com.example.asistenteestudiantil.utils.FirebaseDb
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class HorariosActivity : AppCompatActivity() {

	private lateinit var binding: ActivityHorariosBinding
	private lateinit var materiasAdapter: MateriasAdapter
	private val courses = mutableListOf<Course>()
	private val schedules = mutableListOf<Schedule>()

	// Referencias a los listeners de Firebase para poder removerlos
	private var coursesListener: ValueEventListener? = null
	private var schedulesListener: ValueEventListener? = null
	private var coursesRef: DatabaseReference? = null
	private var schedulesRef: DatabaseReference? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityHorariosBinding.inflate(layoutInflater)
		setContentView(binding.root)

		materiasAdapter = MateriasAdapter(
			onEdit = { showMateriaDialog(it) },
			onDelete = { deleteMateria(it) },
			schedulesProvider = { schedules }
		)
		binding.recyclerMaterias.apply {
			layoutManager = LinearLayoutManager(this@HorariosActivity)
			adapter = materiasAdapter
		}

		binding.fabAdd.setOnClickListener { showMateriaDialog(null) }

		// Configurar tabs
		binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				when (tab?.position) {
					0 -> {
						binding.recyclerMaterias.visibility = View.VISIBLE
						binding.scrollHorario.visibility = View.GONE
					}
					1 -> {
						binding.recyclerMaterias.visibility = View.GONE
						binding.scrollHorario.visibility = View.VISIBLE
						loadWeeklySchedule()
					}
				}
			}
			override fun onTabUnselected(tab: TabLayout.Tab?) {}
			override fun onTabReselected(tab: TabLayout.Tab?) {}
		})

		subscribeToCourses()
		setupBottomNavigation()
	}

	private fun subscribeToCourses() {
		coursesRef = FirebaseDb.coursesRef()
		coursesListener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				// Verificar que la Activity aún esté activa (los callbacks de Firebase ya están en el hilo principal)
				if (isDestroyed || isFinishing) return

				try {
					courses.clear()
					courses.addAll(FirebaseDb.mapCourses(snapshot))
					materiasAdapter.submit(courses)
					// Forzar actualización del adapter para que muestre los horarios actualizados
					if (::materiasAdapter.isInitialized) {
						materiasAdapter.notifyDataSetChanged()
					}
					if (::binding.isInitialized && binding.scrollHorario.visibility == View.VISIBLE) {
						loadWeeklySchedule()
					}
				} catch (e: Exception) {
					// Si hay error, probablemente la vista ya fue destruida
					e.printStackTrace()
				}
			}
			override fun onCancelled(error: DatabaseError) {
				// Log error pero no hacer nada crítico
			}
		}
		coursesRef?.addValueEventListener(coursesListener!!)

		schedulesRef = FirebaseDb.schedulesRef()
		schedulesListener = object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				// Verificar que la Activity aún esté activa (los callbacks de Firebase ya están en el hilo principal)
				if (isDestroyed || isFinishing) return

				try {
					schedules.clear()
					schedules.addAll(FirebaseDb.mapSchedules(snapshot))
					// Actualizar el adapter de materias para que muestre los horarios actualizados
					if (::materiasAdapter.isInitialized) {
						materiasAdapter.notifyDataSetChanged()
					}
					if (::binding.isInitialized && binding.scrollHorario.visibility == View.VISIBLE) {
						loadWeeklySchedule()
					}
				} catch (e: Exception) {
					// Si hay error, probablemente la vista ya fue destruida
					e.printStackTrace()
				}
			}
			override fun onCancelled(error: DatabaseError) {
				// Log error pero no hacer nada crítico
			}
		}
		schedulesRef?.addValueEventListener(schedulesListener!!)
	}

	private fun showMateriaDialog(existing: Course?) {
		val dialogBinding = DialogMateriaBinding.inflate(LayoutInflater.from(this))
		val view = dialogBinding.root

		// Rellenar campos si es edición
		existing?.let {
			dialogBinding.inputNombre.setText(it.name)
			dialogBinding.inputCodigo.setText(it.code)
			dialogBinding.inputProfesor.setText(it.professor)
			dialogBinding.inputUbicacion.setText(it.location)
			dialogBinding.inputCreditos.setText(it.credits.toString())
			dialogBinding.inputSemestre.setText(it.semester)
		}

		// Configurar spinner de días
		val days = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
		dialogBinding.spinnerDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)

		// Variables para almacenar las horas seleccionadas
		var horaInicioSeleccionada: Pair<Int, Int>? = null // (hour, minute)
		var horaFinSeleccionada: Pair<Int, Int>? = null

		// Si es edición, cargar el horario existente y parsear las horas
		existing?.let { course ->
			val existingSchedule = schedules.find { it.courseId == course.id }
			existingSchedule?.let { schedule ->
				// Seleccionar el día
				val dayIndex = (schedule.dayOfWeek - 1).coerceIn(0, 6)
				dialogBinding.spinnerDia.setSelection(dayIndex)
				// Parsear y rellenar horas
				parseTime(schedule.startTime)?.let { (h, m) ->
					horaInicioSeleccionada = Pair(h, m)
					dialogBinding.inputHoraInicio.setText(schedule.startTime)
				}
				parseTime(schedule.endTime)?.let { (h, m) ->
					horaFinSeleccionada = Pair(h, m)
					dialogBinding.inputHoraFin.setText(schedule.endTime)
				}
			}
		}

		// Configurar TimePicker para hora inicio
		dialogBinding.inputHoraInicio.setOnClickListener {
			val hora = horaInicioSeleccionada?.first ?: 9
			val minuto = horaInicioSeleccionada?.second ?: 0
			android.app.TimePickerDialog(
				this,
				{ _, hourOfDay, minute ->
					horaInicioSeleccionada = Pair(hourOfDay, minute)
					val horaFormateada = String.format("%02d:%02d", hourOfDay, minute)
					dialogBinding.inputHoraInicio.setText(horaFormateada)
				},
				hora,
				minuto,
				true // 24 horas
			).show()
		}

		// Configurar TimePicker para hora fin
		dialogBinding.inputHoraFin.setOnClickListener {
			val hora = horaFinSeleccionada?.first ?: 11
			val minuto = horaFinSeleccionada?.second ?: 0
			android.app.TimePickerDialog(
				this,
				{ _, hourOfDay, minute ->
					horaFinSeleccionada = Pair(hourOfDay, minute)
					val horaFormateada = String.format("%02d:%02d", hourOfDay, minute)
					dialogBinding.inputHoraFin.setText(horaFormateada)
				},
				hora,
				minuto,
				true // 24 horas
			).show()
		}

		MaterialAlertDialogBuilder(this)
			.setTitle(if (existing == null) "Nueva Materia" else "Editar Materia")
			.setView(view)
			.setNegativeButton("Cancelar", null)
			.setPositiveButton("Guardar") { _, _ ->
				val nombre = dialogBinding.inputNombre.text.toString().trim()
				val codigo = dialogBinding.inputCodigo.text.toString().trim()
				val profesor = dialogBinding.inputProfesor.text.toString().trim()
				val ubicacion = dialogBinding.inputUbicacion.text.toString().trim()
				val creditos = dialogBinding.inputCreditos.text.toString().toIntOrNull() ?: 0
				val semestre = dialogBinding.inputSemestre.text.toString().trim()
				var horaInicio = dialogBinding.inputHoraInicio.text.toString().trim()
				var horaFin = dialogBinding.inputHoraFin.text.toString().trim()
				val diaSeleccionado = dialogBinding.spinnerDia.selectedItemPosition + 1 // 1=Lunes, 7=Domingo

				if (nombre.isEmpty() || codigo.isEmpty() || profesor.isEmpty() || ubicacion.isEmpty()) {
					Snackbar.make(binding.root, "Por favor completa todos los campos obligatorios", Snackbar.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				// Validar formato de hora y convertir si es necesario
				if (horaInicio.isNotEmpty()) {
					horaInicio = normalizeTimeFormat(horaInicio)
					if (!isValidTimeFormat(horaInicio)) {
						Snackbar.make(binding.root, "Formato de hora inicio inválido. Usa HH:mm (ej: 09:30)", Snackbar.LENGTH_LONG).show()
						return@setPositiveButton
					}
				}

				if (horaFin.isNotEmpty()) {
					horaFin = normalizeTimeFormat(horaFin)
					if (!isValidTimeFormat(horaFin)) {
						Snackbar.make(binding.root, "Formato de hora fin inválido. Usa HH:mm (ej: 11:30)", Snackbar.LENGTH_LONG).show()
						return@setPositiveButton
					}
				}

				// Validar que si hay hora inicio, también haya hora fin y viceversa
				if ((horaInicio.isNotEmpty() && horaFin.isEmpty()) || (horaInicio.isEmpty() && horaFin.isNotEmpty())) {
					Snackbar.make(binding.root, "Debes ingresar ambas horas (inicio y fin) para el horario", Snackbar.LENGTH_LONG).show()
					return@setPositiveButton
				}

				// Validar que hora fin sea después de hora inicio
				if (horaInicio.isNotEmpty() && horaFin.isNotEmpty() && !isTimeBefore(horaInicio, horaFin)) {
					Snackbar.make(binding.root, "La hora de fin debe ser posterior a la hora de inicio", Snackbar.LENGTH_LONG).show()
					return@setPositiveButton
				}

				val course = existing?.copy(
					name = nombre,
					code = codigo,
					professor = profesor,
					location = ubicacion,
					credits = creditos,
					semester = semestre,
					updatedAt = System.currentTimeMillis()
				) ?: Course(
					id = "",
					name = nombre,
					code = codigo,
					professor = profesor,
					location = ubicacion,
					credits = creditos,
					semester = semestre,
					createdAt = System.currentTimeMillis(),
					updatedAt = System.currentTimeMillis()
				)

				// Crear o actualizar el curso y obtener el ID
				val finalCourse = if (existing?.id?.isNotEmpty() == true) {
					course.copy(id = existing.id)
				} else {
					val newId = FirebaseDb.coursesRef().push().key ?: ""
					course.copy(id = newId)
				}

				FirebaseDb.createOrUpdateCourse(finalCourse)

				// Si hay horario completo, guardarlo también
				if (horaInicio.isNotEmpty() && horaFin.isNotEmpty()) {
					val scheduleId = if (existing?.id?.isNotEmpty() == true) {
						// Buscar schedule existente para esta materia
						schedules.find { it.courseId == existing.id }?.id ?: ""
					} else {
						// Para nueva materia, generar un ID temporal que luego Firebase actualizará
						FirebaseDb.schedulesRef().push().key ?: ""
					}

					val schedule = Schedule(
						id = scheduleId,
						courseId = finalCourse.id, // Asegurarse de usar el ID correcto del curso
						courseName = nombre,
						dayOfWeek = diaSeleccionado,
						startTime = horaInicio,
						endTime = horaFin,
						room = ubicacion,
						professor = profesor,
						color = generateColorFromName(nombre),
						createdAt = existing?.createdAt ?: System.currentTimeMillis(),
						updatedAt = System.currentTimeMillis()
					)

					// Guardar en Firebase
					FirebaseDb.createOrUpdateSchedule(schedule)

					// Agregar el schedule localmente temporalmente para que se muestre inmediatamente
					// Esto evita esperar a que Firebase actualice el listener
					val existingIndex = schedules.indexOfFirst {
						it.id == schedule.id && it.id.isNotEmpty() ||
						(it.courseId == schedule.courseId && it.id.isEmpty())
					}
					if (existingIndex >= 0) {
						schedules[existingIndex] = schedule
					} else {
						schedules.add(schedule)
					}
					// Actualizar el adapter inmediatamente
					if (::materiasAdapter.isInitialized) {
						materiasAdapter.notifyDataSetChanged()
					}
				} else if (existing?.id?.isNotEmpty() == true) {
					// Si se está editando y se eliminó el horario, eliminarlo también
					val existingSchedule = schedules.find { it.courseId == existing.id }
					existingSchedule?.let {
						FirebaseDb.deleteSchedule(it.id)
						schedules.remove(it)
						// Actualizar el adapter
						if (::materiasAdapter.isInitialized) {
							materiasAdapter.notifyDataSetChanged()
						}
					}
				}

				Snackbar.make(binding.root, "Materia guardada", Snackbar.LENGTH_SHORT).show()
			}
			.show()
	}

	private fun deleteMateria(course: Course) {
		MaterialAlertDialogBuilder(this)
			.setTitle("Eliminar Materia")
			.setMessage("¿Estás seguro de que deseas eliminar ${course.name}? Se eliminarán también sus horarios y actividades.")
			.setNegativeButton("Cancelar", null)
			.setPositiveButton("Eliminar") { _, _ ->
				// Eliminar schedules relacionados
				schedules.filter { it.courseId == course.id }.forEach {
					FirebaseDb.deleteSchedule(it.id)
				}
				// Eliminar actividades relacionadas
				FirebaseDb.courseActivitiesRef(course.id).addListenerForSingleValueEvent(object : ValueEventListener {
					override fun onDataChange(snapshot: DataSnapshot) {
						snapshot.children.forEach { child ->
							val activity = child.getValue(com.example.asistenteestudiantil.models.AcademicActivity::class.java)
							if (activity != null) {
								FirebaseDb.deleteActivity(activity)
							}
						}
					}
					override fun onCancelled(error: DatabaseError) {}
				})
				FirebaseDb.deleteCourse(course.id)
				Snackbar.make(binding.root, "Materia eliminada", Snackbar.LENGTH_SHORT).show()
			}
			.show()
	}

	override fun onDestroy() {
		super.onDestroy()
		// Remover listeners para evitar memory leaks y errores
		coursesListener?.let { listener ->
			coursesRef?.removeEventListener(listener)
		}
		schedulesListener?.let { listener ->
			schedulesRef?.removeEventListener(listener)
		}
		coursesListener = null
		schedulesListener = null
		coursesRef = null
		schedulesRef = null
	}

	private fun loadWeeklySchedule() {
		// Verificar que la Activity aún esté activa
		if (isDestroyed || isFinishing) return

		// Limpiar horario
		try {
			binding.containerHorario.removeAllViews()
		} catch (e: Exception) {
			// Si hay error, probablemente la vista ya fue destruida
			return
		}

		// Crear grid de horario semanal
		val dias = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
		val horas = (7..21).map { String.format("%02d:00", it) }

		// Crear encabezados de días
		val marginPx = (8 * resources.displayMetrics.density).toInt()
		val headerRow = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			val params = ViewGroup.MarginLayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			params.setMargins(marginPx, marginPx, marginPx, marginPx)
			layoutParams = params
			// Celda vacía para la columna de horas
			addView(TextView(this@HorariosActivity).apply {
				layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
				text = "Hora"
				textSize = 12f
				setTypeface(null, android.graphics.Typeface.BOLD)
				setPadding(8, 8, 8, 8)
			})
			dias.forEach { dia ->
				addView(TextView(this@HorariosActivity).apply {
					layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
					text = dia
					textSize = 12f
					setTypeface(null, android.graphics.Typeface.BOLD)
					setPadding(8, 8, 8, 8)
					setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
				})
			}
		}
		binding.containerHorario.addView(headerRow)

		// Crear filas para cada hora
		horas.forEach { hora ->
			val rowMarginPx = (8 * resources.displayMetrics.density).toInt()
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				val params = ViewGroup.MarginLayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
				params.setMargins(rowMarginPx, rowMarginPx / 2, rowMarginPx, rowMarginPx / 2)
				layoutParams = params

				// Celda de hora
				addView(TextView(this@HorariosActivity).apply {
					layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
					text = hora
					textSize = 10f
					setPadding(8, 8, 8, 8)
				})

				// Celdas para cada día
				dias.forEachIndexed { index, dia ->
					val dayNumber = index + 1
					val materiasEnEstaHoraYDia = schedules.filter { schedule ->
						schedule.dayOfWeek == dayNumber &&
						isTimeInRange(hora, schedule.startTime, schedule.endTime)
					}

					val cellContent = if (materiasEnEstaHoraYDia.isNotEmpty()) {
						// Mostrar las materias de esta hora
						materiasEnEstaHoraYDia.joinToString("\n") { s ->
							"${s.courseName}\n${s.professor}\n${s.room}"
						}
					} else {
						""
					}

					addView(TextView(this@HorariosActivity).apply {
						layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f).apply {
							minHeight = (80 * resources.displayMetrics.density).toInt()
						}
						text = cellContent
						textSize = 9f
						setPadding(4, 4, 4, 4)
						if (cellContent.isNotEmpty()) {
							setBackgroundColor(android.graphics.Color.parseColor("#C5E1A5"))
							setTypeface(null, android.graphics.Typeface.BOLD)
						} else {
							setBackgroundColor(android.graphics.Color.parseColor("#F8FBF8"))
						}
						gravity = Gravity.TOP
					})
				}
			}
			binding.containerHorario.addView(row)
		}
	}

	private fun normalizeTimeFormat(time: String): String {
		// Convertir formatos como "9:30" a "09:30"
		return time.replace(" ", "").let { t ->
			if (t.matches(Regex("^\\d{1,2}:\\d{2}"))) {
				val parts = t.split(":")
				val hour = parts[0].padStart(2, '0')
				val minute = parts[1].padEnd(2, '0').take(2)
				"$hour:$minute"
			} else {
				t
			}
		}
	}

	private fun isValidTimeFormat(time: String): Boolean {
		return time.matches(Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$"))
	}

	private fun isTimeBefore(time1: String, time2: String): Boolean {
		val format = SimpleDateFormat("HH:mm", Locale.getDefault())
		return try {
			val t1 = format.parse(time1)
			val t2 = format.parse(time2)
			t1 != null && t2 != null && t1 < t2
		} catch (e: Exception) {
			false
		}
	}

	private fun isTimeInRange(time: String, start: String, end: String): Boolean {
		val format = SimpleDateFormat("HH:mm", Locale.getDefault())
		return try {
			val timeObj = format.parse(time)
			val startObj = format.parse(start)
			val endObj = format.parse(end)

			if (timeObj == null || startObj == null || endObj == null) {
				return false
			}

			// Verificar que la hora esté dentro del rango (incluyendo los límites)
			// Si la hora es exactamente igual a start o end, también está en rango
			timeObj >= startObj && timeObj <= endObj
		} catch (e: Exception) {
			// Si hay error al parsear, retornar false
			e.printStackTrace()
			false
		}
	}

	private fun generateColorFromName(name: String): Int {
		return name.hashCode() and 0xFFFFFF or 0xFF000000.toInt()
	}

	private fun parseTime(timeString: String): Pair<Int, Int>? {
		return try {
			val parts = timeString.split(":")
			if (parts.size == 2) {
				val hour = parts[0].toInt()
				val minute = parts[1].toInt()
				Pair(hour, minute)
			} else {
				null
			}
		} catch (e: Exception) {
			null
		}
	}

	private fun setupBottomNavigation() {
		findViewById<BottomNavigationView>(R.id.bottomNav)?.apply {
			selectedItemId = R.id.nav_horarios
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_dashboard -> { startActivity(Intent(this@HorariosActivity, DashboardActivity::class.java)); finish(); true }
					R.id.nav_horarios -> true
					R.id.nav_apuntes -> { startActivity(Intent(this@HorariosActivity, ApuntesActivity::class.java)); finish(); true }
					R.id.nav_actividades -> { startActivity(Intent(this@HorariosActivity, ActividadesActivity::class.java)); finish(); true }
					R.id.nav_calendario -> { startActivity(Intent(this@HorariosActivity, CalendarioActivity::class.java)); finish(); true }
					R.id.nav_perfil -> { startActivity(Intent(this@HorariosActivity, PerfilActivity::class.java)); finish(); true }
					else -> false
				}
			}
		}
	}
}

private class MateriasAdapter(
	private val onEdit: (Course) -> Unit,
	private val onDelete: (Course) -> Unit,
	private val schedulesProvider: () -> List<Schedule>
) : RecyclerView.Adapter<MateriaViewHolder>() {

	private var items: List<Course> = emptyList()

	fun submit(list: List<Course>) {
		items = list
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MateriaViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_materia, parent, false)
		return MateriaViewHolder(view, onEdit, onDelete, schedulesProvider)
	}

	override fun getItemCount() = items.size

	override fun onBindViewHolder(holder: MateriaViewHolder, position: Int) {
		holder.bind(items[position])
	}
}

private class MateriaViewHolder(
	itemView: View,
	private val onEdit: (Course) -> Unit,
	private val onDelete: (Course) -> Unit,
	private val schedulesProvider: () -> List<Schedule>
) : RecyclerView.ViewHolder(itemView) {
	private val txtNombre: TextView = itemView.findViewById(R.id.txtNombre)
	private val txtCodigo: TextView = itemView.findViewById(R.id.txtCodigo)
	private val txtProfesor: TextView = itemView.findViewById(R.id.txtProfesor)
	private val txtUbicacion: TextView = itemView.findViewById(R.id.txtUbicacion)
	private val txtHorario: TextView = itemView.findViewById(R.id.txtHorario)
	private val btnEdit: View = itemView.findViewById(R.id.btnEdit)
	private val btnDelete: View = itemView.findViewById(R.id.btnDelete)

	fun bind(course: Course) {
		txtNombre.text = course.name
		txtCodigo.text = course.code
		txtProfesor.text = "Prof: ${course.professor}"
		txtUbicacion.text = "Ubicación: ${course.location}"

		// Buscar el schedule de esta materia para mostrar el horario
		// Usar la lista más reciente de schedules cada vez que se bindea
		val schedule = schedulesProvider().find { it.courseId == course.id }
		if (schedule != null && schedule.startTime.isNotEmpty() && schedule.endTime.isNotEmpty()) {
			val dayNames = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
			val dayName = if (schedule.dayOfWeek >= 1 && schedule.dayOfWeek <= 7) {
				dayNames[schedule.dayOfWeek - 1]
			} else {
				"Día ${schedule.dayOfWeek}"
			}
			txtHorario.text = "Horario: $dayName ${schedule.startTime} - ${schedule.endTime}"
			txtHorario.visibility = View.VISIBLE
		} else {
			txtHorario.text = "Sin horario asignado"
			txtHorario.visibility = View.VISIBLE
		}

		btnEdit.setOnClickListener { onEdit(course) }
		btnDelete.setOnClickListener { onDelete(course) }
	}
}

