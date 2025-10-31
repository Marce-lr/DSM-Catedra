package com.example.asistenteestudiantil

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.asistenteestudiantil.databinding.ActivityApuntesBinding
import com.example.asistenteestudiantil.models.Note
import com.example.asistenteestudiantil.utils.NotesService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ApuntesActivity : AppCompatActivity() {

	private lateinit var binding: ActivityApuntesBinding
	private lateinit var adapter: NotesAdapter
	private val subjectId: String by lazy { intent.getStringExtra("subjectId") ?: "default" }

	private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
		if (uri != null) handlePickedFile(uri)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityApuntesBinding.inflate(layoutInflater)
		setContentView(binding.root)

		adapter = NotesAdapter(onOpen = { openNote(it) }, onDelete = { deleteNote(it) })
		binding.recycler.apply {
			layoutManager = LinearLayoutManager(this@ApuntesActivity)
			adapter = this@ApuntesActivity.adapter
		}

		binding.fabAdd.setOnClickListener { openPicker() }

		NotesService.notesRef().addValueEventListener(object : ValueEventListener {
			override fun onDataChange(snapshot: DataSnapshot) {
				val list = NotesService.map(snapshot).filter { it.subjectId == subjectId }
				adapter.submit(list)
			}

			override fun onCancelled(error: DatabaseError) {}
		})

		findViewById<BottomNavigationView>(R.id.bottomNav).apply {
			selectedItemId = R.id.nav_apuntes
			setOnItemSelectedListener { item ->
				when (item.itemId) {
					R.id.nav_apuntes -> true
					R.id.nav_actividades -> { startActivity(Intent(this@ApuntesActivity, ActividadesActivity::class.java)); true }
					R.id.nav_perfil -> { startActivity(Intent(this@ApuntesActivity, PerfilActivity::class.java)); true }
					else -> false
				}
			}
		}
	}

	private fun openPicker() {
		pickFileLauncher.launch(arrayOf("application/pdf", "image/*", "audio/*", "text/plain"))
	}

	private fun handlePickedFile(uri: Uri) {
		contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
		val meta = queryMeta(uri)
		binding.progress.visibility = View.VISIBLE
		NotesService.uploadNote(
			subjectId = subjectId,
			fileUri = uri,
			fileName = meta.first,
			mimeType = meta.second,
			sizeBytes = meta.third
		) { result ->
			binding.progress.visibility = View.GONE
			result.onFailure { Snackbar.make(binding.root, "Error al subir: ${it.message}", Snackbar.LENGTH_LONG).show() }
			result.onSuccess { Snackbar.make(binding.root, "Apunte subido", Snackbar.LENGTH_SHORT).show() }
		}
	}

	private fun openNote(note: Note) {
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(note.downloadUrl)))
	}

	private fun deleteNote(note: Note) {
		NotesService.deleteNote(note) { result ->
			result.onFailure { Snackbar.make(binding.root, "Error al eliminar: ${it.message}", Snackbar.LENGTH_LONG).show() }
		}
	}

	private fun queryMeta(uri: Uri): Triple<String, String, Long> {
		var name = "archivo"
		var size = 0L
		contentResolver.query(uri, null, null, null, null)?.use { cursor ->
			val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
			val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
			if (cursor.moveToFirst()) {
				if (nameIndex >= 0) name = cursor.getString(nameIndex)
				if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
			}
		}
		val mime = contentResolver.getType(uri) ?: "application/octet-stream"
		return Triple(name, mime, size)
	}
}

private class NotesAdapter(
	private val onOpen: (Note) -> Unit,
	private val onDelete: (Note) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<NoteVH>() {

	private var items: List<Note> = emptyList()

	fun submit(list: List<Note>) { items = list; notifyDataSetChanged() }

	override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NoteVH {
		val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
		return NoteVH(v, onOpen, onDelete)
	}

	override fun getItemCount(): Int = items.size

	override fun onBindViewHolder(holder: NoteVH, position: Int) { holder.bind(items[position]) }
}

private class NoteVH(
	itemView: View,
	private val onOpen: (Note) -> Unit,
	private val onDelete: (Note) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
	private val title: TextView = itemView.findViewById(R.id.txtTitle)
	private val subtitle: TextView = itemView.findViewById(R.id.txtSubtitle)
	private val btnOpen: View = itemView.findViewById(R.id.btnOpen)
	private val btnDelete: View = itemView.findViewById(R.id.btnDelete)
	fun bind(n: Note) {
		title.text = n.name
		subtitle.text = "${n.mimeType} • ${(n.sizeBytes/1024).coerceAtLeast(1)} KB"
		btnOpen.setOnClickListener { onOpen(n) }
		btnDelete.setOnClickListener { onDelete(n) }
	}
}


