package com.procurement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class BookingRequest {

        @NotNull(message = "Schedule ID is required")
        private Long scheduleId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(
                value = "0.01",
                message = "Quantity must be greater than 0"
        )
        private BigDecimal quantityQuintal;

        // Default constructor
        public BookingRequest() {
        }

        // Getter for scheduleId
        public Long getScheduleId() {
                return scheduleId;
        }

        // Setter for scheduleId
        public void setScheduleId(Long scheduleId) {
                this.scheduleId = scheduleId;
        }

        // Getter for quantityQuintal
        public BigDecimal getQuantityQuintal() {
                return quantityQuintal;
        }

        // Setter for quantityQuintal
        public void setQuantityQuintal(BigDecimal quantityQuintal) {
                this.quantityQuintal = quantityQuintal;
        }
}