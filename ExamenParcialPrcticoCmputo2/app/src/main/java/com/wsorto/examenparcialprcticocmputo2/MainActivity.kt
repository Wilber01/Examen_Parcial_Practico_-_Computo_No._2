package com.wsorto.examenparcialprcticocmputo2

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: ConexionSQLite
    private lateinit var recyclerMovimientos: RecyclerView
    private lateinit var adapter: MovimientoAdapter
    private lateinit var tvIngresos: TextView
    private lateinit var tvGastos: TextView
    private lateinit var tvSaldo: TextView
    private lateinit var tvFechaActual: TextView
    private lateinit var etBusqueda: EditText
    private lateinit var btnAgregar: FloatingActionButton

    private var listaMovimientos: List<Movimiento> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar base de datos
        dbHelper = ConexionSQLite(this)

        // Inicializar vistas
        initViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar listeners
        setupListeners()

        // Cargar datos iniciales
        cargarDatos()
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun initViews() {
        recyclerMovimientos = findViewById(R.id.recycler_movimientos)
        tvIngresos = findViewById(R.id.tv_ingresos)
        tvGastos = findViewById(R.id.tv_gastos)
        tvSaldo = findViewById(R.id.tv_saldo)
        tvFechaActual = findViewById(R.id.tv_fecha_actual)
        etBusqueda = findViewById(R.id.et_busqueda)
        btnAgregar = findViewById(R.id.btn_agregar)

        // Establecer fecha actual
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        tvFechaActual.text = "Fecha: $fechaActual"
    }

    private fun setupRecyclerView() {
        recyclerMovimientos.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        // Botón agregar
        btnAgregar.setOnClickListener {
            val intent = Intent(this, RegistrarActivity::class.java)
            startActivity(intent)
        }

        // Búsqueda en tiempo real
        etBusqueda.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val busqueda = s.toString().trim()
                if (busqueda.isEmpty()) {
                    cargarDatos()
                } else {
                    buscarMovimientos(busqueda)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun cargarDatos() {
        lifecycleScope.launch {
            val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Cargar movimientos
            listaMovimientos = obtenerMovimientosAsync()

            // Cargar resumen
            val resumen = obtenerResumenAsync(fechaActual)

            withContext(Dispatchers.Main) {
                // Actualizar RecyclerView
                adapter = MovimientoAdapter(
                    listaMovimientos,
                    onEdit = { movimiento -> editarMovimiento(movimiento) },
                    onDelete = { movimiento -> confirmarEliminar(movimiento) }
                )
                recyclerMovimientos.adapter = adapter

                // Actualizar resumen
                tvIngresos.text = String.format("$%.2f", resumen.totalIngresos)
                tvGastos.text = String.format("$%.2f", resumen.totalGastos)
                tvSaldo.text = String.format("$%.2f", resumen.saldo)

                // Cambiar color del saldo según sea positivo o negativo
                tvSaldo.setTextColor(
                    if (resumen.saldo >= 0)
                        getColor(android.R.color.holo_green_dark)
                    else
                        getColor(android.R.color.holo_red_dark)
                )
            }
        }
    }

    private fun buscarMovimientos(busqueda: String) {
        lifecycleScope.launch {
            val resultados = withContext(Dispatchers.IO) {
                dbHelper.readableDatabase.use { db ->
                    dbHelper.buscarMovimientos(db, busqueda)
                }
            }

            withContext(Dispatchers.Main) {
                adapter = MovimientoAdapter(
                    resultados,
                    onEdit = { movimiento -> editarMovimiento(movimiento) },
                    onDelete = { movimiento -> confirmarEliminar(movimiento) }
                )
                recyclerMovimientos.adapter = adapter
            }
        }
    }

    private fun editarMovimiento(movimiento: Movimiento) {
        val intent = Intent(this, RegistrarActivity::class.java)
        intent.putExtra("id", movimiento.id)
        intent.putExtra("tipo", movimiento.tipo)
        intent.putExtra("descripcion", movimiento.descripcion)
        intent.putExtra("categoria", movimiento.categoria)
        intent.putExtra("monto", movimiento.monto)
        intent.putExtra("fecha", movimiento.fecha)
        startActivity(intent)
    }

    private fun confirmarEliminar(movimiento: Movimiento) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Movimiento")
            .setMessage("¿Está seguro que desea eliminar este movimiento?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarMovimiento(movimiento.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarMovimiento(id: Int) {
        lifecycleScope.launch {
            val eliminado = withContext(Dispatchers.IO) {
                dbHelper.writableDatabase.use { db ->
                    dbHelper.eliminarMovimiento(db, id)
                }
            }

            withContext(Dispatchers.Main) {
                if (eliminado) {
                    Toast.makeText(this@MainActivity, "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@MainActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun obtenerMovimientosAsync(): List<Movimiento> {
        return withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.use { db ->
                dbHelper.obtenerTodosMovimientos(db)
            }
        }
    }

    private suspend fun obtenerResumenAsync(fecha: String): ResumenDiario {
        return withContext(Dispatchers.IO) {
            dbHelper.readableDatabase.use { db ->
                dbHelper.obtenerResumenPorFecha(db, fecha)
            }
        }
    }
}

// Adapter para RecyclerView
class MovimientoAdapter(
    private val movimientos: List<Movimiento>,
    private val onEdit: (Movimiento) -> Unit,
    private val onDelete: (Movimiento) -> Unit
) : RecyclerView.Adapter<MovimientoAdapter.MovimientoViewHolder>() {

    class MovimientoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tv_tipo)
        val tvDescripcion: TextView = view.findViewById(R.id.tv_descripcion)
        val tvCategoria: TextView = view.findViewById(R.id.tv_categoria)
        val tvMonto: TextView = view.findViewById(R.id.tv_monto)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val btnEditar: ImageButton = view.findViewById(R.id.btn_editar)
        val btnEliminar: ImageButton = view.findViewById(R.id.btn_eliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        val movimiento = movimientos[position]

        holder.tvTipo.text = movimiento.tipo
        holder.tvDescripcion.text = movimiento.descripcion
        holder.tvCategoria.text = movimiento.categoria
        holder.tvMonto.text = String.format("$%.2f", movimiento.monto)
        holder.tvFecha.text = movimiento.fecha

        // Color según tipo
        if (movimiento.tipo == "Ingreso") {
            holder.tvTipo.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_green_light))
            holder.tvMonto.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else {
            holder.tvTipo.setBackgroundColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
            holder.tvMonto.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        }

        // Listeners
        holder.btnEditar.setOnClickListener { onEdit(movimiento) }
        holder.btnEliminar.setOnClickListener { onDelete(movimiento) }
    }

    override fun getItemCount() = movimientos.size
}