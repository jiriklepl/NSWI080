# This script to be run in the documentation folder after the files (see below) are generated

library(stringr)
library(tidyverse)
library(dplyr)
library(ggplot2)

files <- list.files(pattern="remote_searcher_\\d+_\\d+")
per_edges <- c()
distance_counts <- c()
distance_total <- c()
remote_distance_total <- c()
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

        t <- remote_distance_total[file_data$Distance[i]]
        remote_distance_total[file_data$Distance[i]] <- file_data$RTime[i]
        if (!is.na(t) && length(t) > 0)
            remote_distance_total[file_data$Distance[i]] <- t + remote_distance_total[file_data$Distance[i]]
    }

    per_edges$Time[length(per_edges$Time) + 1] <- sum(file_data$Time) / length(file_data$Time)
    per_edges$Density[length(per_edges$Density) + 1] <- 2 * numbers[2] / (numbers[1] * (numbers[1] - 1))
    per_edges$RTime[length(per_edges$RTime) + 1] <- sum(file_data$RTime) / length(file_data$RTime)
}

per_edges <- per_edges %>% as_tibble %>% arrange(Density)
per_edges %>% ggplot(aes(x=Density)) + (geom_point(aes(y=Time, color="Time"))) + (geom_point(aes(y=RTime, color="Remote time")))
ggsave("remote_searcher1.png")

distance_average <- ifelse(distance_counts > 0, distance_total / distance_counts, NA)
remote_distance_average <- ifelse(distance_counts > 0, remote_distance_total / distance_counts, NA)
png("remote_searcher2_1.png")
plot(x=(1:(distance_average %>% length)) - 1, y=distance_average, xlab="Distance (0 denotes infinity)", ylab="Average time")
dev.off()
png("remote_searcher2_2.png")
plot(x=(1:(distance_average %>% length)) - 1, y=remote_distance_average, xlab="Distance (0 denotes infinity)", ylab="Average remote time")
dev.off()
