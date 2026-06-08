package com.example.trackflow;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Interface Retrofit untuk dua API berbasis OpenStreetMap:
 *
 * 1. Nominatim API  -> Geocoding / Pencarian lokasi teks ke koordinat lat/lon
 *    Base URL: https://nominatim.openstreetmap.org/
 *
 * 2. OSRM API       -> Routing / Menghitung jalur + jarak + waktu antar dua titik
 *    Base URL: https://router.project-osrm.org/
 *
 * Catatan: Keduanya bersumber dari data OpenStreetMap (OSM) dan gratis digunakan.
 */
public interface NominatimApiService {

    /**
     * Nominatim: Cari agen jasa ekspedisi berdasarkan kata kunci dan kota.
     * Contoh: getLocations("jasa ekspedisi", "Makassar", "json", 1, 10)
     *
     * @param query     Kata kunci pencarian, misalnya "jasa ekspedisi JNE"
     * @param format    Format hasil — selalu "json"
     * @param addressdetails 1 = sertakan detail alamat lengkap
     * @param limit     Jumlah hasil maksimum
     * @return List of NominatimPlace objects
     */
    @GET("search")
    Call<List<NominatimPlace>> searchPlaces(
            @Query("q") String query,
            @Query("format") String format,
            @Query("addressdetails") int addressdetails,
            @Query("limit") int limit
    );

    /**
     * OSRM: Hitung rute dari titik A (lokasiku) ke titik B (agen ekspedisi).
     * Endpoint penuh diberikan via @Url karena base URL berbeda dengan Nominatim.
     *
     * Format URL: /route/v1/driving/{lon_asal},{lat_asal};{lon_tujuan},{lat_tujuan}?overview=full&geometries=geojson&steps=false
     *
     * @param url URL penuh endpoint OSRM yang sudah di-build di MapFragment
     * @return OsrmResponse berisi jarak (meter) dan durasi (detik)
     */
    @GET
    Call<OsrmResponse> getRoute(@Url String url);
}
