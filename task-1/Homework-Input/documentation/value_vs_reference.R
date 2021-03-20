# This script to be run in the documentation folder after the files (see below) are generated

library(stringr)
library(tidyverse)
library(dplyr)
library(ggplot2)

files <- list.files(pattern="value_vs_reference_\\d+_\\d+")

distance_counts <- c()
remote_searcher_distance_counts <- c()
remote_nodes_distance_counts <- c()
remote_searcher_distance_t_total <- c()

distance_total <- c()
remote_searcher_distance_total <- c()
remote_nodes_distance_total <- c()
remote_nodes_distance_t_total <- c()

process_file <- function(file_data, distance_counts, distance_total, distance_t_total) {
    for(i in 1:length(file_data$Distance)) {
        if (length(file_data$Distance[i]) == 0)
            next;

        distance <- as.numeric(file_data$Distance[i]) + 1
        if (distance == 0) distance <- 1

        t <- distance_counts[distance]
        distance_counts[distance] <- 1
        if (!is.na(t) && length(t) > 0)
            distance_counts[distance] <- t + distance_counts[distance]

        if (length(file_data$Time[i]) > 0) {
            t <- distance_total[distance]
            distance_total[distance] <- file_data$Time[i]
            if (!is.na(t) && length(t) > 0)
                distance_total[distance] <- t + distance_total[distance]
        }

        if (length(file_data$TTime[i]) > 0) {
            t <- distance_t_total[distance]
            distance_t_total[distance] <- file_data$TTime[i]
            if (!is.na(t) && length(t) > 0)
                distance_t_total[distance] <- t + distance_t_total[distance]
        }
    }

    return (list("distance_counts" = distance_counts, "distance_total" = distance_total, "distance_t_total" = distance_t_total))
}

for (file in files) {
    numbers <- as.numeric(str_extract_all(file, '\\d+')[[1]])
    file_data <- read_table(file, n_max=50) %>% mutate (Time = as.numeric(Time)) %>% drop_na
    rtrn <- process_file(file_data, remote_searcher_distance_counts, remote_searcher_distance_total, remote_searcher_distance_t_total)
    remote_searcher_distance_counts <- rtrn$distance_counts
    remote_searcher_distance_total <- rtrn$distance_total
    remote_searcher_distance_t_total <- rtrn$distance_t_total
    file_data <- read_table(file, skip=51, n_max=50) %>% mutate (Time = as.numeric(Time)) %>% drop_na
    rtrn <- process_file(file_data, remote_nodes_distance_counts, remote_nodes_distance_total, remote_nodes_distance_t_total)
    remote_nodes_distance_counts <- rtrn$distance_counts
    remote_nodes_distance_total <- rtrn$distance_total
    remote_nodes_distance_t_total <- rtrn$distance_t_total
}

remote_searcher_distance_average <- ifelse(remote_searcher_distance_counts > 0, remote_searcher_distance_total / remote_searcher_distance_counts, NA)
remote_searcher_distance_t_average <- ifelse(remote_searcher_distance_counts > 0, remote_searcher_distance_t_total / remote_searcher_distance_counts, NA)
remote_searcher_data = cbind(Distance = (1:(remote_searcher_distance_average %>% length)) - 1, Average_time = remote_searcher_distance_average, Average_t_time = remote_searcher_distance_t_average)

remote_nodes_distance_average <- ifelse(remote_nodes_distance_counts > 0, remote_nodes_distance_total / remote_nodes_distance_counts, NA)
remote_nodes_distance_t_average <- ifelse(remote_nodes_distance_counts > 0, remote_nodes_distance_t_total / remote_nodes_distance_counts, NA)
remote_nodes_data = cbind(Distance = (1:(remote_nodes_distance_average %>% length)) - 1, Average_time = remote_nodes_distance_average, Average_t_time = remote_nodes_distance_t_average)

ggplot(NULL, aes(x=Distance)) +
    geom_jitter(data=as.data.frame(remote_searcher_data), aes(y=Average_time, color="Average remote searcher time")) +
    geom_jitter(data=as.data.frame(remote_searcher_data), aes(y=Average_t_time, color="Average remote searcher transitive time")) +
    geom_jitter(data=as.data.frame(remote_nodes_data), aes(y=Average_time, color="Average remote nodes time")) +
    geom_jitter(data=as.data.frame(remote_nodes_data), aes(y=Average_t_time, color="Average remote nodes transitive time")) +
    labs(x="Distance (0 denotes infinity)", y="Average time")
ggsave("value_vs_reference2.png")
