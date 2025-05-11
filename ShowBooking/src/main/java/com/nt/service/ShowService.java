package com.nt.service;

import com.nt.exception.BookingException;
import com.nt.exception.ShowNotFoundException;
import com.nt.model.Booking;
import com.nt.model.Show;
import com.nt.model.WaitlistEntry;
import com.nt.repository.BookingRepository;
import com.nt.repository.ShowRepository;
import com.nt.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShowService {
    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    public String registerShow(String name, String genre) {
    	System.out.println("ShowService.registerShow()");
        Show show = new Show();
        show.setName(name);
        show.setGenre(genre);
        showRepository.save(show);
        return name + " show is registered !!"  ;
    }

    public String onboardShowSlots(String name, Map<String, Integer> slots) {
    	System.out.println("ShowService.onboardShowSlots()");
        Show show = showRepository.findByName(name);
        if (show == null) {
            throw new ShowNotFoundException("Show not found!");
        }
        show.getSlots().putAll(slots);
        System.out.println(showRepository.findAll());
        return "Done!";
    }
    
    public List<String> showAvailabilityByGenre(String genre) {
    	System.out.println("ShowService.showAvailabilityByGenre()");
    	System.out.println("Fetching shows for genre: " + genre);
        System.out.println("Current repository state: " + showRepository.findAll());
        List<String> list =  showRepository.findAll().values().stream()
                .filter(show -> show.getGenre().equals(genre))
                .map(show -> show.getName() + ": " + show.getSlots())
                .collect(Collectors.toList());
        System.out.println("list of avilabalit" + list.toString());
        return list ;
    }

    public String bookTicket(String user, String showName, String slot, int persons) {
        Show show = showRepository.findByName(showName);
        if (show == null) {
            throw new ShowNotFoundException("Show not found!");
        }
        if (!show.getSlots().containsKey(slot) || show.getSlots().get(slot) < persons) {
            WaitlistEntry entry = new WaitlistEntry();
            entry.setUser(user);
            entry.setShowName(showName);
            entry.setSlot(slot);
            entry.setPersons(persons);
            waitlistRepository.add(entry);
            return "Slot is full. Added to waitlist.";
        }
        String bookingId = UUID.randomUUID().toString();
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUser(user);
        booking.setShowName(showName);
        booking.setSlot(slot);
        booking.setPersons(persons);
        bookingRepository.save(booking);

        int remainingCapacity = show.getSlots().get(slot) - persons;
        show.getSlots().put(slot, remainingCapacity);
        return "Booked. Booking id: " + bookingId;
    }

    public String cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .orElseThrow(() -> new BookingException("Booking not found!"));

        Show show = showRepository.findByName(booking.getShowName());
        int updatedCapacity = show.getSlots().get(booking.getSlot()) + booking.getPersons();
        show.getSlots().put(booking.getSlot(), updatedCapacity);
        bookingRepository.delete(booking);

        WaitlistEntry entry = waitlistRepository.findFirst(booking.getShowName(), booking.getSlot());
        if (entry != null) {
            bookTicket(entry.getUser(), entry.getShowName(), entry.getSlot(), entry.getPersons());
            waitlistRepository.remove(entry);
        }
        return "Booking canceled.";
    }
}
