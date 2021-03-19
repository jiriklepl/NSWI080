# This script to be run in the documentation folder

library(stringr)
library(tidyverse)
library(dplyr)
library(ggplot2)

files <- list.files(pattern="local_measurement_\\d+_\\d+")
per_edges <- c()
distance_counts <- c()
distance_total <- c()
for (file in files) {
    numbers <- as.numeric(str_extract_all(file, '\\d+')[[1]])
    file_data <- read_table(file)

    for(i in 1:length(file_data$Distance)) {
        if (i == -1) i <- 1
        else i <- i + 1

        t <- distance_counts[file_data$Distance[i]]
        distance_counts[file_data$Distance[i]] <- 1
        if (!is.na(t) && length(t) > 0)
            distance_counts[file_data$Distance[i]] <- t + distance_counts[file_data$Distance[i]]

        t <- distance_total[file_data$Distance[i]]
        distance_total[file_data$Distance[i]] <- file_data$Time[i]
        if (!is.na(t) && length(t) > 0)
            distance_total[file_data$Distance[i]] <- t + distance_total[file_data$Distance[i]]
    }

    per_edges$Time[length(per_edges$Time) + 1] <- sum(file_data$Time) / length(file_data$Time)
    per_edges$Density[length(per_edges$Density) + 1] <- 2 * numbers[2] / (numbers[1] * (numbers[1] - 1))
}

per_edges <- per_edges %>% as_tibble %>% arrange(Density)
per_edges %>% ggplot(aes(x=Density, y=Time)) + geom_point()
ggsave("local_measurements1.png")

distance_average <- ifelse(distance_counts > 0, distance_total / distance_counts, NA)
png("local_measurements2.png")
plot(x=(1:(distance_average %>% length)) - 1, y=distance_average, xlab="Distance (0 denotes infinity)", ylab="Average time")
dev.off()
