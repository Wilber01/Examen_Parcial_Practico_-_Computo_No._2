package com.wsorto.examenparcialprcticocmputo2

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ResumenActivity : AppCompatActivity() {

    private lateinit var dbHelper: ConexionSQLite
    private lateinit var etFechaResumen: EditText
    private lateinit var btnBuscarResumen: Button
    private lateinit var tvFechaSeleccionada: TextView
    private lateinit var tvTotalIngresos: TextView
    private lateinit var tvTotalGastos: TextView
    private lateinit var tvSaldoDia: TextView
    private lateinit var recyclerMovimientosDia: RecyclerView
    private lateinit var tvSinMovimientos: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen)

        supportActionBar?.title = "Resumen de Movimientos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar base de datos
        dbHelper = ConexionSQLite(this)

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupListeners()

        // Cargar resumen de hoy por defecto
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etFechaResumen.setText(fechaActual)
        cargarResumen(fechaActual)
    }

    private fun initViews() {
        etFechaResumen = findViewById(R.id.et_fecha_resumen)
        btnBuscarResumen = findViewById(R.id.btn_buscar_resumen)
        tvFechaSeleccionada = findViewById(R.id.tv_fecha_seleccionada)
        tvTotalIngresos = findViewById(R.id.tv_total_ingresos)
        tvTotalGastos = findViewById(R.id.tv_total_gastos)
        tvSaldoDia = findViewById(R.id.tv_saldo_dia)
        recyclerMovimientosDia = findViewById(R.id.recycler_movimientos_dia)
        tvSinMovimientos = findViewById(R.id.tv_sin_movimientos)

        recyclerMovimientosDia.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        // Selector de fecha
        etFechaResumen.setOnClickListener {
            mostrarDatePicker()
        }

        // Botón buscar
        btnBuscarResumen.setOnClickListener {
            val fecha = etFechaResumen.text.toString()
            if (fecha.isNotEmpty()) {
                cargarResumen(fecha)
            }
        }
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()

        val fechaActual = etFechaResumen.text.toString()
        if (fechaActual.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                calendar.time = sdf.parse(fechaActual) ?: Date()
            } catch (e: Exception) {
                // Usar fecha actual si hay error
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val fecha = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                etFechaResumen.setText(fecha)
                cargarResumen(fecha)
            },
            year, month, day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun cargarResumen(fecha: String) {
        lifecycleScope.launch {
            // Obtener resumen
            val resumen = withContext(Dispatchers.IO) {
                dbHelper.readableDatabase.use { db ->
                    dbHelper.obtenerResumenPorFecha(db, fecha)
                }
            }

            // Obtener movimientos del día
            val movimientos = withContext(Dispatchers.IO) {
                dbHelper.readableDatabase.use { db ->
                    val cursor = db.rawQuery(
                        "SELECT * FROM ${ConexionSQLite.TABLE_MOVIMIENTOS} WHERE ${ConexionSQLite.COLUMN_FECHA} = ? ORDER BY ${ConexionSQLite.COLUMN_ID} DESC",
                        arrayOf(fecha)
                    )
                    val lista = mutableListOf<Movimiento>()

                    cursor.use {
                        if (it.moveToFirst()) {
                            do {
                                val id = it.getInt(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_ID))
                                val tipo = it.getString(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_TIPO))
                                val descripcion = it.getString(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_DESCRIPCION))
                                val categoria = it.getString(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_CATEGORIA))
                                val monto = it.getDouble(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_MONTO))
                                val fechaMov = it.getString(it.getColumnIndexOrThrow(ConexionSQLite.COLUMN_FECHA))

                                lista.add(Movimiento(id, tipo, descripcion, categoria, monto, fechaMov))
                            } while (it.moveToNext())
                        }
                    }
                    lista
                }
            }

            withContext(Dispatchers.Main) {
                // Actualizar UI
                tvFechaSeleccionada.text = "Fecha: $fecha"
                tvTotalIngresos.text = String.format("$%.2f", resumen.totalIngresos)
                tvTotalGastos.text = String.format("$%.2f", resumen.totalGastos)
                tvSaldoDia.text = String.format("$%.2f", resumen.saldo)

                // Cambiar color del saldo
                tvSaldoDia.setTextColor(
                    if (resumen.saldo >= 0)
                        getColor(android.R.color.holo_green_dark)
                    else
                        getColor(android.R.color.holo_red_dark)
                )

                // Mostrar movimientos
                if (movimientos.isEmpty()) {
                    recyclerMovimientosDia.visibility = View.GONE
                    tvSinMovimientos.visibility = View.VISIBLE
                } else {
                    recyclerMovimientosDia.visibility = View.VISIBLE
                    tvSinMovimientos.visibility = View.GONE

                    val adapter = MovimientoResumenAdapter(movimientos)
                    recyclerMovimientosDia.adapter = adapter
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

// Adapter simplificado para el resumen (sin botones de editar/eliminar)
class MovimientoResumenAdapter(
    private val movimientos: List<Movimiento>
) : RecyclerView.Adapter<MovimientoResumenAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tv_tipo_resumen)
        val tvDescripcion: TextView = view.findViewById(R.id.tv_descripcion_resumen)
        val tvCategoria: TextView = view.findViewById(R.id.tv_categoria_resumen)
        val tvMonto: TextView = view.findViewById(R.id.tv_monto_resumen)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento_resumen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movimiento = movimientos[position]

        holder.tvTipo.text = movimiento.tipo
        holder.tvDescripcion.text = movimiento.descripcion
        holder.tvCategoria.text = movimiento.categoria
        holder.tvMonto.text = String.format("$%.2f", movimiento.monto)

        // Color según tipo
        if (movimiento.tipo == "Ingreso") {
            holder.tvTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_green_light)
            )
            holder.tvMonto.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_green_dark)
            )
        } else {
            holder.tvTipo.setBackgroundColor(
                holder.itemView.context.getColor(android.R.color.holo_red_light)
            )
            holder.tvMonto.setTextColor(
                holder.itemView.context.getColor(android.R.color.holo_red_dark)
            )
        }
    }

    override fun getItemCount() = movimientos.size
}