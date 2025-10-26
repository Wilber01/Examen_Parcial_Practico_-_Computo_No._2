package com.wsorto.examenparcialprcticocmputo2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ConexionSQLite(
    context: Context,
    name: String = "control_dinero.db",
    factory: SQLiteDatabase.CursorFactory? = null,
    version: Int = 1
) : SQLiteOpenHelper(context, name, factory, version) {

    companion object {
        const val TABLE_MOVIMIENTOS = "movimientos"
        const val COLUMN_ID = "id"
        const val COLUMN_TIPO = "tipo"
        const val COLUMN_DESCRIPCION = "descripcion"
        const val COLUMN_CATEGORIA = "categoria"
        const val COLUMN_MONTO = "monto"
        const val COLUMN_FECHA = "fecha"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_MOVIMIENTOS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIPO TEXT NOT NULL,
                $COLUMN_DESCRIPCION TEXT NOT NULL,
                $COLUMN_CATEGORIA TEXT NOT NULL,
                $COLUMN_MONTO REAL NOT NULL,
                $COLUMN_FECHA TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MOVIMIENTOS")
        onCreate(db)
    }

    // Insertar movimiento
    fun insertarMovimiento(
        db: SQLiteDatabase,
        tipo: String,
        descripcion: String,
        categoria: String,
        monto: Double,
        fecha: String
    ): Boolean {
        return try {
            val insertSQL = """
                INSERT INTO $TABLE_MOVIMIENTOS 
                ($COLUMN_TIPO, $COLUMN_DESCRIPCION, $COLUMN_CATEGORIA, $COLUMN_MONTO, $COLUMN_FECHA) 
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            db.execSQL(insertSQL, arrayOf(tipo, descripcion, categoria, monto, fecha))
            true
        } catch (e: Exception) {
            false
        }
    }

    // Obtener todos los movimientos
    fun obtenerTodosMovimientos(db: SQLiteDatabase): List<Movimiento> {
        val cursor = db.rawQuery("SELECT * FROM $TABLE_MOVIMIENTOS ORDER BY $COLUMN_FECHA DESC", null)
        val movimientos = mutableListOf<Movimiento>()

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                    val tipo = it.getString(it.getColumnIndexOrThrow(COLUMN_TIPO))
                    val descripcion = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPCION))
                    val categoria = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORIA))
                    val monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_MONTO))
                    val fecha = it.getString(it.getColumnIndexOrThrow(COLUMN_FECHA))

                    movimientos.add(Movimiento(id, tipo, descripcion, categoria, monto, fecha))
                } while (it.moveToNext())
            }
        }
        return movimientos
    }

    // Actualizar movimiento
    fun actualizarMovimiento(
        db: SQLiteDatabase,
        id: Int,
        tipo: String,
        descripcion: String,
        categoria: String,
        monto: Double,
        fecha: String
    ): Boolean {
        return try {
            val updateSQL = """
                UPDATE $TABLE_MOVIMIENTOS 
                SET $COLUMN_TIPO = ?, $COLUMN_DESCRIPCION = ?, $COLUMN_CATEGORIA = ?, 
                    $COLUMN_MONTO = ?, $COLUMN_FECHA = ?
                WHERE $COLUMN_ID = ?
            """.trimIndent()
            db.execSQL(updateSQL, arrayOf(tipo, descripcion, categoria, monto, fecha, id))
            true
        } catch (e: Exception) {
            false
        }
    }

    // Eliminar movimiento
    fun eliminarMovimiento(db: SQLiteDatabase, id: Int): Boolean {
        return try {
            val deleteSQL = "DELETE FROM $TABLE_MOVIMIENTOS WHERE $COLUMN_ID = ?"
            db.execSQL(deleteSQL, arrayOf(id))
            true
        } catch (e: Exception) {
            false
        }
    }

    // Obtener resumen por fecha
    fun obtenerResumenPorFecha(db: SQLiteDatabase, fecha: String): ResumenDiario {
        var totalIngresos = 0.0
        var totalGastos = 0.0

        val cursor = db.rawQuery(
            "SELECT $COLUMN_TIPO, SUM($COLUMN_MONTO) as total FROM $TABLE_MOVIMIENTOS WHERE $COLUMN_FECHA = ? GROUP BY $COLUMN_TIPO",
            arrayOf(fecha)
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val tipo = it.getString(it.getColumnIndexOrThrow(COLUMN_TIPO))
                    val total = it.getDouble(it.getColumnIndexOrThrow("total"))

                    if (tipo == "Ingreso") {
                        totalIngresos = total
                    } else {
                        totalGastos = total
                    }
                } while (it.moveToNext())
            }
        }

        val saldo = totalIngresos - totalGastos
        return ResumenDiario(totalIngresos, totalGastos, saldo)
    }

    // Buscar movimientos
    fun buscarMovimientos(db: SQLiteDatabase, busqueda: String): List<Movimiento> {
        val cursor = db.rawQuery(
            """SELECT * FROM $TABLE_MOVIMIENTOS 
               WHERE $COLUMN_DESCRIPCION LIKE ? OR $COLUMN_CATEGORIA LIKE ?
               ORDER BY $COLUMN_FECHA DESC""",
            arrayOf("%$busqueda%", "%$busqueda%")
        )

        val movimientos = mutableListOf<Movimiento>()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                    val tipo = it.getString(it.getColumnIndexOrThrow(COLUMN_TIPO))
                    val descripcion = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPCION))
                    val categoria = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORIA))
                    val monto = it.getDouble(it.getColumnIndexOrThrow(COLUMN_MONTO))
                    val fecha = it.getString(it.getColumnIndexOrThrow(COLUMN_FECHA))

                    movimientos.add(Movimiento(id, tipo, descripcion, categoria, monto, fecha))
                } while (it.moveToNext())
            }
        }
        return movimientos
    }
}

// Data classes
data class Movimiento(
    val id: Int,
    val tipo: String,
    val descripcion: String,
    val categoria: String,
    val monto: Double,
    val fecha: String
)

data class ResumenDiario(
    val totalIngresos: Double,
    val totalGastos: Double,
    val saldo: Double
)