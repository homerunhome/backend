package org.zerozero.homerunhome.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerozero.homerunhome.itinerary.domain.Itinerary;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {}
