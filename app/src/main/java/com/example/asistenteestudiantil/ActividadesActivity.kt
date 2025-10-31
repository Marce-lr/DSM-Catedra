package com.example.asistenteestudiantil

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
	private lateinit var subjectId: String

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityActividadesBinding.inflate(layoutInflater)
		setContentView(binding.root)

		subjectId = intent.getStringExtra("subjectId") ?: "default"

		adapter = ActividadAdapter(onEdit = { showUpsertDialog(it) }, onDelete = {
			FirebaseDb.deleteActivity(it.subjectId, it.id)
		})
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
					R.id.nav_actividades -> true
					R.id.nav_apuntes -> { startActivity(android.content.Intent(this@ActividadesActivity, ApuntesActivity::class.java)); true }
					R.id.nav_perfil -> {
						startActivity(android.content.Intent(this@ActividadesActivity, PerfilActivity::class.java))
						true
					}
					else -> false
				}
			}
		}
	}

	private fun subscribeToActivities() {
		FirebaseDb.activitiesRef(subjectId).addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				val list = FirebaseDb.mapActivities(snapshot)
				adapter.submit(list)
				applyFilters()
			}

			override fun onCancelled(error: DatabaseError) {}
		})
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
		val dialogBinding = DialogActividadBinding.inflate(LayoutInflater.from(this))
		val view: View = dialogBinding.root

		dialogBinding.inputTitle.setText(existing?.title ?: "")
		dialogBinding.inputDescription.setText(existing?.description ?: "")
		dialogBinding.inputWeight.setText((existing?.weightPercent ?: 0.0).toString())
		dialogBinding.inputScore.setText(existing?.scoreObtained?.toString() ?: "")

		dialogBinding.spinnerType.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityType.values().map { it.name }
		)
		dialogBinding.spinnerPriority.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityPriority.values().map { it.name }
		)
		dialogBinding.spinnerStatus.adapter = ArrayAdapter(
			this,
			android.R.layout.simple_spinner_dropdown_item,
			ActivityStatus.values().map { it.name }
		)

		MaterialAlertDialogBuilder(this)
			.setTitle(if (existing == null) getString(R.string.actividad_nueva) else getString(R.string.actividad_editar))
			.setView(view)
			.setNegativeButton(R.string.cancelar, null)
			.setPositiveButton(R.string.guardar) { _, _ ->
				val now = System.currentTimeMillis()
				val act = AcademicActivity(
					id = existing?.id ?: "",
					subjectId = subjectId,
					title = dialogBinding.inputTitle.text.toString().trim(),
					description = dialogBinding.inputDescription.text.toString().trim(),
					type = ActivityType.valueOf(dialogBinding.spinnerType.selectedItem as String),
					priority = ActivityPriority.valueOf(dialogBinding.spinnerPriority.selectedItem as String),
					status = ActivityStatus.valueOf(dialogBinding.spinnerStatus.selectedItem as String),
					// fecha en millis desde DatePicker simple (usa hoy si no se cambia)
					dueDateMillis = now,
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

	fun submit(list: List<AcademicActivity>) {
		items = list
		notifyDataSetChanged()
	}

	override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ActividadViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		val v = inflater.inflate(R.layout.item_actividad, parent, false)
		return ActividadViewHolder(v, onEdit, onDelete)
	}

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: ActividadViewHolder, position: Int) {
		holder.bind(items[position])
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

	fun bind(item: AcademicActivity) {
		title.text = item.title
		subtitle.text = itemView.context.getString(
			R.string.item_actividad_subtitle,
			item.type.name,
			item.priority.name,
			item.status.name,
			item.weightPercent
		)
		btnEdit.setOnClickListener { onEdit(item) }
		btnDelete.setOnClickListener { onDelete(item) }
	}
}


