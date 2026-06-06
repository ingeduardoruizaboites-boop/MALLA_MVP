package com.malla.mvp.core.engine

/**
 * Niveles de transporte disponibles en MALLA.
 * Ordenados de mayor a menor capacidad/ancho de banda.
 */
enum class MeshLevel(val label: String) {
    ONLINE_WIFI("WiFi"),
    ONLINE_MOBILE("Datos móviles"),
    WIFI_DIRECT("Wi-Fi Direct"),
    BLE("Bluetooth LE"),
    BLUETOOTH_CLASSIC("BT Clásico"),
    NFC("NFC"),
    ULTRASOUND("Audio ultrasónico"),
    SMS_BRIDGE("Puente SMS"),
    FLASH_LIGHT("Señal óptica"),
    QR_CODE("QR"),
    NO_SIGNAL("Sin señal")
}
