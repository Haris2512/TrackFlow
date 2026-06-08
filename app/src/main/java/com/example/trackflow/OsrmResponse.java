package com.example.trackflow;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Model data untuk response dari OSRM Routing API.
 *
 * Struktur JSON OSRM (disederhanakan):
 * {
 *   "code": "Ok",
 *   "routes": [
 *     {
 *       "distance": 3500.5,   <-- dalam METER
 *       "duration": 420.0,    <-- dalam DETIK
 *       "geometry": { ... }   <-- GeoJSON LineString koordinat rute (tidak kita parse di sini)
 *     }
 *   ]
 * }
 *
 * Kita ambil routes[0] untuk mendapatkan rute terbaik/terpendek.
 */
public class OsrmResponse {

    @SerializedName("code")
    private String code;

    @SerializedName("routes")
    private List<Route> routes;

    public String getCode() { return code; }

    public List<Route> getRoutes() { return routes; }

    /** Ambil rute pertama (terbaik) dari hasil OSRM */
    public Route getBestRoute() {
        if (routes != null && !routes.isEmpty()) return routes.get(0);
        return null;
    }

    // ======================== Inner Class: Route ========================

    public static class Route {

        @SerializedName("distance")
        private double distance; // dalam METER

        @SerializedName("duration")
        private double duration; // dalam DETIK

        @SerializedName("geometry")
        private Geometry geometry;

        public double getDistance() { return distance; }

        public double getDuration() { return duration; }

        public Geometry getGeometry() { return geometry; }

        /** Konversi jarak dari meter ke kilometer, format 1 desimal */
        public String getDistanceKm() {
            return String.format("%.1f km", distance / 1000.0);
        }

        /** Konversi durasi dari detik ke menit, dibulatkan ke atas */
        public String getDurationMinutes() {
            long minutes = (long) Math.ceil(duration / 60.0);
            if (minutes < 60) {
                return minutes + " menit";
            } else {
                long hours = minutes / 60;
                long sisa = minutes % 60;
                return sisa > 0 ? hours + " jam " + sisa + " menit" : hours + " jam";
            }
        }
    }

    // ======================== Inner Class: Geometry ========================

    public static class Geometry {

        @SerializedName("coordinates")
        private List<List<Double>> coordinates; // [[lon,lat], [lon,lat], ...]

        @SerializedName("type")
        private String type;

        public List<List<Double>> getCoordinates() { return coordinates; }

        public String getType() { return type; }
    }
}
