package com.wsorto.examenparcialprcticocmputo2

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RegistrarActivity : AppCompatActivity() {

    private lateinit var dbHelper: ConexionSQLite
    private lateinit var rgTipo: RadioGroup
    private lateinit var rbIngreso: RadioButton
    private lateinit var rbGasto: RadioButton
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var etMonto: TextInputEditText
    private lateinit var etFecha: EditText
    private lateinit var btnCalendario: ImageButton
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    private var movimientoId: Int? = null
    private var modoEdicion = false

    // Categorías predefinidas
    private val categoriasIngreso = arrayOf("Venta", "Salario", "Prestamo", "Otros Ingresos")
    private val categoriasGasto = arrayOf("Transporte", "Comida", "Servicios", "Compras", "Otros Gastos")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registrar)

        // Inicializar base de datos
        dbHelper = ConexionSQLite(this)

        // Inicializar vistas
        initViews()

        // Configurar listeners
        setupListeners()

        // Verificar si es modo edición
        verificarModoEdicion()

        // Configurar fecha por defecto
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etFecha.setText(fechaActual)

        // Configurar categorías iniciales
        actualizarCategorias()
    }

    private fun initViews() {
        rgTipo = findViewById(R.id.rg_tipo)
        rbIngreso = findViewById(R.id.rb_ingreso)
        rbGasto = findViewById(R.id.rb_gasto)
        etDescripcion = findViewById(R.id.et_descripcion)
        spinnerCategoria = findViewById(R.id.spinner_categoria)
        etMonto = findViewById(R.id.et_monto)
        etFecha = findViewById(R.id.et_fecha)
        btnCalendario = findViewById(R.id.btn_calendario)
        btnGuardar = findViewById(R.id.btn_guardar)
        btnCancelar = findViewById(R.id.btn_cancelar)

        // Cambiar título según modo
        supportActionBar?.title = if (modoEdicion) "Editar Movimiento" else "Registrar Movimiento"
    }

    private fun setupListeners() {
        // Cambiar categorías según tipo seleccionado
        rgTipo.setOnCheckedChangeListener { _, checkedId ->
            actualizarCategorias()
        }

        // Selector de fecha
        btnCalendario.setOnClickListener {
            mostrarDatePicker()
        }

        etFecha.setOnClickListener {
            mostrarDatePicker()
        }

        // Botón guardar
        btnGuardar.setOnClickListener {
            if (validarCampos()) {
                guardarMovimiento()
            }
        }

        // Botón cancelar
        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun verificarModoEdicion() {
        movimientoId = intent.getIntExtra("id", -1)
        if (movimientoId != -1) {
            modoEdicion = true
            cargarDatosMovimiento()
        }
    }

    private fun cargarDatosMovimiento() {
        val tipo = intent.getStringExtra("tipo") ?: "Ingreso"
        val descripcion = intent.getStringExtra("descripcion") ?: ""
        val categoria = intent.getStringExtra("categoria") ?: ""
        val monto = intent.getDoubleExtra("monto", 0.0)
        val fecha = intent.getStringExtra("fecha") ?: ""

        // Establecer tipo
        if (tipo == "Ingreso") {
            rbIngreso.isChecked = true
        } else {
            rbGasto.isChecked = true
        }

        // Establecer datos
        etDescripcion.setText(descripcion)
        etMonto.setText(monto.toString())
        etFecha.setText(fecha)

        // Actualizar categorías y seleccionar la correcta
        actualizarCategorias()
        val categorias = if (tipo == "Ingreso") categoriasIngreso else categoriasGasto
        val posicion = categorias.indexOf(categoria)
        if (posicion != -1) {
            spinnerCategoria.setSelection(posicion)
        }

        btnGuardar.text = "Actualizar"
    }

    private fun actualizarCategorias() {
        val categorias = if (rbIngreso.isChecked) {
            categoriasIngreso
        } else {
            categoriasGasto
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapter
    }

    private fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()

        // Si hay fecha en el campo, usarla como inicial
        val fechaActual = etFecha.text.toString()
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
                etFecha.setText(fecha)
            },
            year, month, day
        )

        // Solo permitir fechas pasadas y actuales
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun validarCampos(): Boolean {
        // Validar descripción
        if (etDescripcion.text.isNullOrEmpty()) {
            etDescripcion.error = "La descripción es obligatoria"
            etDescripcion.requestFocus()
            return false
        }

        // Validar monto
        val montoStr = etMonto.text.toString()
        if (montoStr.isEmpty()) {
            etMonto.error = "El monto es obligatorio"
            etMonto.requestFocus()
            return false
        }

        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            etMonto.error = "Ingrese un monto válido mayor a 0"
            etMonto.requestFocus()
            return false
        }

        // Validar fecha
        if (etFecha.text.isNullOrEmpty()) {
            Toast.makeText(this, "Seleccione una fecha", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar formato de fecha
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.isLenient = false
            sdf.parse(etFecha.text.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha inválido (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun guardarMovimiento() {
        val tipo = if (rbIngreso.isChecked) "Ingreso" else "Gasto"
        val descripcion = etDescripcion.text.toString().trim()
        val categoria = spinnerCategoria.selectedItem.toString()
        val monto = etMonto.text.toString().toDouble()
        val fecha = etFecha.text.toString()

        lifecycleScope.launch {
            val resultado = if (modoEdicion && movimientoId != null) {
                // Actualizar
                withContext(Dispatchers.IO) {
                    dbHelper.writableDatabase.use { db ->
                        dbHelper.actualizarMovimiento(db, movimientoId!!, tipo, descripcion, categoria, monto, fecha)
                    }
                }
            } else {
                // Insertar nuevo
                withContext(Dispatchers.IO) {
                    dbHelper.writableDatabase.use { db ->
                        dbHelper.insertarMovimiento(db, tipo, descripcion, categoria, monto, fecha)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (resultado) {
                    val mensaje = if (modoEdicion) "Movimiento actualizado" else "Movimiento registrado"
                    Toast.makeText(this@RegistrarActivity, mensaje, Toast.LENGTH_SHORT).show()

                    // Limpiar campos si es registro nuevo
                    if (!modoEdicion) {
                        limpiarCampos()
                    } else {
                        finish()
                    }
                } else {
                    Toast.makeText(this@RegistrarActivity, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun limpiarCampos() {
        etDescripcion.setText("")
        etMonto.setText("")
        spinnerCategoria.setSelection(0)
        etDescripcion.requestFocus()

        // Mantener fecha actual
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        etFecha.setText(fechaActual)
    }
}