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
import java.io.File

class ApuntesActivity : AppCompatActivity() {

	private lateinit var binding: ActivityApuntesBinding
	private lateinit var adapter: NotesAdapter
	private val subjectId: String by lazy { intent.getStringExtra("subjectId") ?: "default" }

	private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
					R.id.nav_dashboard -> { startActivity(Intent(this@ApuntesActivity, DashboardActivity::class.java)); finish(); true }
					R.id.nav_apuntes -> true
					R.id.nav_actividades -> { startActivity(Intent(this@ApuntesActivity, ActividadesActivity::class.java)); finish(); true }
					R.id.nav_horarios -> { startActivity(Intent(this@ApuntesActivity, HorariosActivity::class.java)); finish(); true }
					R.id.nav_calendario -> { startActivity(Intent(this@ApuntesActivity, CalendarioActivity::class.java)); finish(); true }
					R.id.nav_perfil -> { startActivity(Intent(this@ApuntesActivity, PerfilActivity::class.java)); finish(); true }
					else -> false
				}
			}
		}
	}

	private fun openPicker() {
		pickFileLauncher.launch("*/*")
	}

	private fun handlePickedFile(uri: Uri) {
		try {
			val meta = queryMeta(uri)
			val originalName = if (meta.first.isNotEmpty() && meta.first != "archivo") {
				meta.first
			} else {
				"apunte_${System.currentTimeMillis()}.${getFileExtension(meta.second)}"
			}
			// Sanitizar el nombre del archivo para evitar caracteres problemáticos
			val fileName = sanitizeFileName(originalName)
			binding.progress.visibility = View.VISIBLE
			NotesService.uploadNote(
				subjectId = subjectId,
				fileUri = uri,
				fileName = fileName,
				mimeType = meta.second,
				sizeBytes = meta.third,
				contentResolver = contentResolver,
				context = this@ApuntesActivity
			) { result ->
				binding.progress.visibility = View.GONE
				result.onFailure { e ->
					val errorMsg = when {
						e.message?.contains("404") == true || e.message?.contains("Not Found") == true ->
							"Error: Verifica las reglas de seguridad de Firebase Storage. El servidor no puede encontrar el recurso."
						e.message?.contains("permission") == true || e.message?.contains("Permission") == true ->
							"Error: No tienes permisos para subir archivos. Verifica tu autenticación."
						else -> e.message ?: "Error desconocido al subir archivo"
					}
					Snackbar.make(binding.root, errorMsg, Snackbar.LENGTH_LONG).show()
					e.printStackTrace()
				}
				result.onSuccess { Snackbar.make(binding.root, "Apunte subido correctamente", Snackbar.LENGTH_SHORT).show() }
			}
		} catch (e: Exception) {
			binding.progress.visibility = View.GONE
			Snackbar.make(binding.root, "Error al procesar archivo: ${e.message}", Snackbar.LENGTH_LONG).show()
			e.printStackTrace()
		}
	}
	
	private fun sanitizeFileName(fileName: String): String {
		// Reemplazar caracteres problemáticos y espacios
		return fileName
			.replace(Regex("[^a-zA-Z0-9._-]"), "_") // Reemplazar caracteres especiales con guión bajo
			.replace(Regex("_{2,}"), "_") // Reemplazar múltiples guiones bajos con uno solo
			.trim()
			.ifEmpty { "archivo_${System.currentTimeMillis()}" }
	}
	
	private fun getFileExtension(mimeType: String): String {
		return when {
			mimeType.startsWith("image/") -> {
				when {
					mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
					mimeType.contains("png") -> "png"
					mimeType.contains("gif") -> "gif"
					mimeType.contains("webp") -> "webp"
					else -> "img"
				}
			}
			mimeType == "application/pdf" -> "pdf"
			mimeType.startsWith("audio/") -> "mp3"
			mimeType.startsWith("text/") -> "txt"
			else -> "bin"
		}
	}

	private fun openNote(note: Note) {
		if (note.localFilePath.isEmpty()) {
			Snackbar.make(binding.root, "No se encontró el archivo", Snackbar.LENGTH_SHORT).show()
			return
		}
		
		val file = File(note.localFilePath)
		if (!file.exists()) {
			Snackbar.make(binding.root, "El archivo ya no existe", Snackbar.LENGTH_SHORT).show()
			return
		}
		
		try {
			val uri = androidx.core.content.FileProvider.getUriForFile(
				this,
				"${packageName}.fileprovider",
				file
			)
			val intent = Intent(Intent.ACTION_VIEW).apply {
				setDataAndType(uri, note.mimeType)
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
			startActivity(intent)
		} catch (e: Exception) {
			Snackbar.make(binding.root, "Error al abrir archivo: ${e.message}", Snackbar.LENGTH_LONG).show()
		}
	}

	private fun deleteNote(note: Note) {
		NotesService.deleteNote(note, this) { result ->
			result.onFailure { 
			Snackbar.make(binding.root, "Error al eliminar: ${it.message}", Snackbar.LENGTH_LONG).show() 
		}
		}
	}

	private fun queryMeta(uri: Uri): Triple<String, String, Long> {
		var name = ""
		var size = 0L
		try {
			contentResolver.query(uri, null, null, null, null)?.use { cursor ->
				val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
				if (cursor.moveToFirst()) {
					if (nameIndex >= 0) {
						val retrievedName = cursor.getString(nameIndex)
						if (retrievedName != null) name = retrievedName
					}
					if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
				}
			}
			// Si no se obtuvo el nombre, intentar extraerlo del URI
			if (name.isEmpty()) {
				val path = uri.path
				if (path != null) {
					val segments = path.split("/")
					if (segments.isNotEmpty()) {
						name = segments.last()
					}
				}
			}
		} catch (e: Exception) {
			// Si hay error, usar un nombre por defecto
			if (name.isEmpty()) {
				name = "archivo_${System.currentTimeMillis()}"
			}
		}
		val mime = try {
			contentResolver.getType(uri) ?: "application/octet-stream"
		} catch (e: Exception) {
			"application/octet-stream"
		}
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


